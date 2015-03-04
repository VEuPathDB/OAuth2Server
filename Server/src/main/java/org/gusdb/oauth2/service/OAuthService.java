package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
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
import org.gusdb.oauth2.server.OAuthServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthService.class);

  private static final String FORM_ID_PARAM_NAME = "form_id";
  private static enum LoginFormStatus { failed; }

  @Context
  private ServletContext _servletContext;

  @Context
  private HttpServletRequest _request;

  @Context
  private HttpHeaders _headers;

  @Context
  private HttpSession _session;

  @GET
  @Path("assets/{name:.+}")
  public Response getStaticFile(@PathParam("name") String name) {
    LOG.info("Request made to fetch resource: " + name);
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
      @HeaderParam("Referer") final String referrer) throws Exception {
    String formId = getFormIdFromReferrer(referrer);
    Session session = new Session(_session);
    Authenticator authenticator = OAuthServlet.getAuthenticator(_servletContext);
    boolean validCreds = authenticator.isCredentialsValid(username, password);
    if (validCreds) {
      session.setUsername(username);
      OAuthAuthzRequest originalRequest = (formId == null ? null : session.clearFormId(formId));
      if (originalRequest == null) {
        // formId doesn't exist on this session; give user generic success page
        return Response.seeOther(new URI("assets/success.html")).build();
      }
      return OAuthRequestHandler.handleAuthorizationRequest(originalRequest, username);
    }
    else {
      return Response.seeOther(getLoginUri(formId, LoginFormStatus.failed)).build();
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
    new Session(_session).invalidate();
    URI uri = (redirectUri == null || redirectUri.isEmpty() ? getLoginUri(null, null) : new URI(redirectUri));
    return Response.seeOther(uri).build();
  }

  @GET
  @Path("/authorize")
  public Response authorize() throws URISyntaxException, OAuthSystemException, OAuthProblemException {
    LOG.info("Handling authorize request with the following params:" +
        System.lineSeparator() + paramsToString(_request));
    OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(_request);
    ClientValidator clientValidator = OAuthServlet.getClientValidator(_servletContext);
    if (!clientValidator.isValidAuthorizationClient(oauthRequest)) {
      return new OAuthResponseFactory().buildInvalidClientResponse();
    }
    Session session = new Session(_session);
    if (session.isAuthenticated()) {
      // user is already logged in; respond with auth code for user
      return OAuthRequestHandler.handleAuthorizationRequest(oauthRequest, session.getUsername());
    }
    else {
      // no one is logged in; generate form ID and send
      return Response.seeOther(getLoginUri(session.generateFormId(oauthRequest), null)).build();
    }
  }

  private static URI getLoginUri(String formId, LoginFormStatus status) throws URISyntaxException {
    String queryString = "";
    if (formId != null) {
      queryString = FORM_ID_PARAM_NAME + "=" + formId;
    }
    if (status != null) {
      queryString += (queryString.isEmpty() ? "" : "&") + "status=" + status.name();
    }
    return new URI("assets/login.html" + (queryString.isEmpty() ? "" : "?" + queryString));
  }

  @POST
  @Path("/token")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getToken(MultivaluedMap<String, String> formParams) throws OAuthSystemException, OAuthProblemException {
    // for POST + URL-encoded form, must use custom HttpServletRequest with Jersey to read actual params
    HttpServletRequest request = new JerseyHttpRequestWrapper(_request, formParams);
    LOG.info("Handling token request with the following params:" +
        System.lineSeparator() + paramsToString(request));
    OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
    ClientValidator clientValidator = OAuthServlet.getClientValidator(_servletContext);
    if (!clientValidator.isValidTokenClient(oauthRequest)) {
      return new OAuthResponseFactory().buildInvalidClientResponse();
    }
    return OAuthRequestHandler.handleTokenRequest(oauthRequest,
        OAuthServlet.getAuthenticator(_servletContext));
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
  public Response getUserInfo() throws OAuthSystemException, OAuthProblemException {
    OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(_request, ParameterStyle.HEADER);
    return OAuthRequestHandler.handleUserInfoRequest(oauthRequest, OAuthServlet.getAuthenticator(_servletContext));
  }

}
