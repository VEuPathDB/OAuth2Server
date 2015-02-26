package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

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
    LOG.info("Handling authorize request");
    return OAuthRequestHandler.handleAuthorizationRequest(_request);
  }

  @POST
  @Path("/token")
  @Consumes("application/x-www-form-urlencoded")
  @Produces("application/json")
  public Response getToken() throws OAuthSystemException {
    return OAuthRequestHandler.handleTokenRequest(_request,
        OAuthServlet.getApplicationConfig(_servletContext),
        OAuthServlet.getAuthenticator(_servletContext),
        new OAuthResponseFactory());
  }

  @GET
  @Path("/user")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserInfo() throws OAuthSystemException {
    return OAuthRequestHandler.handleUserInfoRequest(_request);
  }
}
