package org.gusdb.oauth2.service;

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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Variant;

import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.assets.StaticResource;
import org.gusdb.oauth2.server.OAuthServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthService.class);

  @Context
  private ServletContext _servletContext;

  @Context
  private HttpServletRequest _request;

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
      @FormParam("redirectUrl") String redirectUrl) throws Exception {
    Authenticator authenticator = OAuthServlet.getAuthenticator(_servletContext);
    boolean validCreds = authenticator.isCredentialsValid(username, password);
    return Response.seeOther(getLoginAttemptResultUrl(validCreds, redirectUrl)).build();
  }

  private URI getLoginAttemptResultUrl(boolean validCreds, String redirectUrl) throws URISyntaxException {
    if (validCreds) {
      if (redirectUrl == null || redirectUrl.isEmpty()) redirectUrl = "assets/success.html";
      return new URI(redirectUrl);
    }
    String redirectUrlParam = (redirectUrl == null || redirectUrl.isEmpty() ? "" : "&redirectUrl=" + redirectUrl);
    return new URI("assets/login.html?status=failed" + redirectUrlParam);
  }

  @GET
  @Path("/logout")
  public Response logOut() throws URISyntaxException {
    StateCache.logOut(_request);
    return Response.seeOther(new URI("assets/login.html")).build();
  }

  @GET
  @Path("/authorize")
  public Response authorize() throws URISyntaxException, OAuthSystemException {
    LOG.info("Handling authorize request with the following params:" +
        System.lineSeparator() + paramsToString(_request));
    return OAuthRequestHandler.handleAuthorizationRequest(_request);
  }

  @POST
  @Path("/token")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getToken(MultivaluedMap<String, String> formParams) throws OAuthSystemException {
    HttpServletRequest wrapper = new JerseyHttpRequestWrapper(_request, formParams);
    LOG.info("Can we get grant type as form param? " + formParams.getFirst("grant_type"));
    LOG.info("Handling token request with the following params:" +
        System.lineSeparator() + paramsToString(wrapper));
    return OAuthRequestHandler.handleTokenRequest(wrapper,
        OAuthServlet.getApplicationConfig(_servletContext),
        OAuthServlet.getAuthenticator(_servletContext),
        new OAuthResponseFactory());
  }

  private static String paramsToString(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Map<String, String[]> params = request.getParameterMap();
    StringBuilder sb = new StringBuilder("{").append(System.lineSeparator());
    for (Entry<String, String[]> entry : params.entrySet()) {
      sb.append("  ").append(entry.getKey()).append(": ").append(Arrays.toString(entry.getValue()));
      //[").append(entry.getValue()[0]);
      //for (int i = 1; i < entry.getValue().length; i++) {
      //  sb.append(", ").append(entry.getValue()[i]);
      //}
      sb./*append("]").*/append(System.lineSeparator());
    }
    return sb.append("}").toString();
  }

  @GET
  @Path("/user")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserInfo() throws OAuthSystemException {
    return OAuthRequestHandler.handleUserInfoRequest(_request);
  }
}
