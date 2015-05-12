package org.gusdb.oauth2.service;

import static org.gusdb.oauth2.assets.StaticResource.RESOURCE_PREFIX;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.assets.StaticResource;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.server.OAuthServlet;
import org.gusdb.oauth2.service.util.AuthzRequest;
import org.gusdb.oauth2.service.util.JerseyHttpRequestWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthService.class);

  private static final String FORM_ID_PARAM_NAME = "form_id";
  private static enum LoginFormStatus { failed, error, accessdenied; }

  @Context
  private ServletContext _context;

  @Context
  private HttpServletRequest _request;

  @Context
  private HttpHeaders _headers;

  @GET
  @Path(RESOURCE_PREFIX + "{name:.+}")
  public Response getStaticFile(@PathParam("name") String name) {
    LOG.debug("Request made to fetch resource: " + name);
    if (name == null || name.isEmpty()) {
      return Response.notAcceptable(Collections.<Variant>emptyList()).build();
    }
    StaticResource resource = new StaticResource(name);
    if (resource.isValid()) {
      return Response.ok(resource.getStreamingOutput()).type(resource.getMimeType()).build();
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @POST
  @Path("login")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response attemptLogin(
      @FormParam("username") final String username,
      @FormParam("password") final String password,
      @HeaderParam("Referer") final String referrer) throws URISyntaxException {
    Session session = new Session(_request.getSession());
    ApplicationConfig config = OAuthServlet.getApplicationConfig(_context);
    String formId = getFormIdFromReferrer(referrer);
    try {
      if ((formId == null || !session.isFormId(formId)) && !config.anonymousLoginsAllowed()) {
        return Response.seeOther(getLoginUri(formId, LoginFormStatus.accessdenied)).build();
      }
      Authenticator authenticator = OAuthServlet.getAuthenticator(_context);
      boolean validCreds = authenticator.isCredentialsValid(username, password);
      if (validCreds) {
        session.setUsername(username);
        AuthzRequest originalRequest = (formId == null ? null : session.clearFormId(formId));
        if (originalRequest == null) {
          // formId doesn't exist on this session; give user generic success page
          return Response.seeOther(new URI(RESOURCE_PREFIX +
              config.getLoginSuccessPage())).build();
        }
        return OAuthRequestHandler.handleAuthorizationRequest(originalRequest,
            username, config.getTokenExpirationSecs());
      }
      else {
        return Response.seeOther(getLoginUri(formId, LoginFormStatus.failed)).build();
      }
    }
    catch (Exception e) {
      LOG.error("Error processing /login request", e);
      return Response.seeOther(getLoginUri(formId, LoginFormStatus.error)).build();
    }
  }

  // FIXME: this could probably be done much better with a regex; fix later
  private String getFormIdFromReferrer(String referrer) {
    String formIdPrefix = FORM_ID_PARAM_NAME + "=";
    if (referrer == null) return null;
    int beginIndex = referrer.indexOf(formIdPrefix);
    if (beginIndex == -1) return null;
    referrer = referrer.substring(beginIndex + formIdPrefix.length());
    if (referrer.isEmpty() || referrer.charAt(0) == '&') return null;
    int endIndex = referrer.indexOf("&");
    if (endIndex == -1) return referrer;
    return referrer.substring(0, endIndex);
  }

  @GET
  @Path("/logout")
  public Response logOut(@QueryParam("redirect_uri") String redirectUri) throws URISyntaxException {
    new Session(_request.getSession()).invalidate();
    URI uri = (redirectUri == null || redirectUri.isEmpty() ? getLoginUri(null, null) : new URI(redirectUri));
    return Response.seeOther(uri).build();
  }

  @GET
  @Path("/authorize")
  public Response authorize() throws URISyntaxException, OAuthSystemException, OAuthProblemException {
    LOG.debug("Handling authorize request with the following params:" +
        System.lineSeparator() + paramsToString(_request));
    OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(_request);
    ClientValidator clientValidator = OAuthServlet.getClientValidator(_context);
    if (!clientValidator.isValidAuthorizationClient(oauthRequest)) {
      return new OAuthResponseFactory().buildInvalidClientResponse();
    }
    Session session = new Session(_request.getSession());
    if (session.isAuthenticated()) {
      // user is already logged in; respond with auth code for user
      ApplicationConfig config = OAuthServlet.getApplicationConfig(_context);
      return OAuthRequestHandler.handleAuthorizationRequest(new AuthzRequest(oauthRequest),
          session.getUsername(), config.getTokenExpirationSecs());
    }
    else {
      // no one is logged in; generate form ID and send
      return Response.seeOther(getLoginUri(session.generateFormId(new AuthzRequest(oauthRequest)), null)).build();
    }
  }

  private URI getLoginUri(String formId, LoginFormStatus status) throws URISyntaxException {
    String queryString = "";
    if (formId != null) {
      queryString = FORM_ID_PARAM_NAME + "=" + formId;
    }
    if (status != null) {
      queryString += (queryString.isEmpty() ? "" : "&") + "status=" + status.name();
    }
    return new URI(RESOURCE_PREFIX + OAuthServlet.getApplicationConfig(_context).getLoginFormPage() +
        (queryString.isEmpty() ? "" : "?" + queryString));
  }

  @POST
  @Path("/token")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getToken(MultivaluedMap<String, String> formParams) throws OAuthSystemException, OAuthProblemException {
    // for POST + URL-encoded form, must use custom HttpServletRequest with Jersey to read actual params
    HttpServletRequest request = new JerseyHttpRequestWrapper(_request, formParams);
    LOG.debug("Handling token request with the following params:" +
        System.lineSeparator() + paramsToString(request));
    OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
    ClientValidator clientValidator = OAuthServlet.getClientValidator(_context);
    if (!clientValidator.isValidTokenClient(oauthRequest)) {
      return new OAuthResponseFactory().buildInvalidClientResponse();
    }
    ApplicationConfig config = OAuthServlet.getApplicationConfig(_context);
    return OAuthRequestHandler.handleTokenRequest(oauthRequest,
        OAuthServlet.getAuthenticator(_context),
        config.getTokenExpirationSecs(),
        config.includeUserInfoWithToken());
  }

  private static String paramsToString(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, String[]> params = request.getParameterMap();
    StringBuilder sb = new StringBuilder("{").append(System.lineSeparator());
    for (Entry<String, String[]> entry : params.entrySet()) {
      sb.append("  ").append(entry.getKey()).append(": ")
        .append(Arrays.toString(entry.getValue())).append(System.lineSeparator());
    }
    return sb.append("}").toString();
  }

  @GET
  @Path("/user")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserInfo() throws Exception {
    OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(_request, ParameterStyle.HEADER);
    return OAuthRequestHandler.handleUserInfoRequest(oauthRequest, OAuthServlet.getAuthenticator(_context));
  }

}
