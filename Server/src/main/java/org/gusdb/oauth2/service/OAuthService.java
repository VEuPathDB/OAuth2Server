package org.gusdb.oauth2.service;

import java.util.Collections;

import javax.json.Json;
import javax.json.JsonObject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.gusdb.oauth2.ApplicationConfig;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.OAuthServlet;
import org.gusdb.oauth2.assets.StaticResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/")
public class OAuthService {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthService.class);

  @Context
  private HttpServletRequest _request;

  @Context
  private UriInfo _uriInfo;

  @Context
  private ServletContext _servletContext;

  @GET
  @Path("asset")
  public Response getStaticFile(@QueryParam("name") String name) {
    LOG.info("Request made to fetch resource: " + name);
    if (name == null || name.isEmpty()) {
      return Response.notAcceptable(Collections.<Variant>emptyList()).build();
    }
    StaticResource resource = new StaticResource(name);
    LOG.info("Created static resource object from name.  Valid = " + resource.isValid());
    if (resource.isValid()) {
      return Response.ok(resource.getStreamingOutput()).type(resource.getMimeType()).build();
    }
    return Response.status(Status.NOT_FOUND).build();
  }

  @POST
  @Path("login")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response attemptLogin(
      @FormParam("username") final String username,
      @FormParam("password") final String password,
      @FormParam("redirectUrl") final String redirectUrl) throws Exception {
    LOG.info("Login form submitted with values: " + username + ", " + password + ", " + redirectUrl);
    Authenticator authenticator = OAuthServlet.getAuthenticator(_servletContext);
    boolean validCreds = authenticator.isCredentialsValid(username, password);
    JsonObject result = Json.createObjectBuilder()
        .add("valid", validCreds)
        .add("redirectUrl", (redirectUrl == null ? "/" : redirectUrl))
        .build();
    return Response.ok(result.toString()).build();
  }
  
  @GET
  @Path("configuration")
  @Produces(MediaType.TEXT_PLAIN)
  public Response getApplicationInfo() {
    try {
      ApplicationConfig config = OAuthServlet.getApplicationConfig(_servletContext);
      Authenticator authenticator = OAuthServlet.getAuthenticator(_servletContext);
      return Response.ok(getConfigText(config, authenticator)).build();
    }
    catch (Exception e) {
      LOG.error("Error fetching configuration", e);
      return Response.serverError().build();
    }
  }

  private String getConfigText(ApplicationConfig config, Authenticator authenticator) throws Exception {
    StringBuilder str = new StringBuilder();
    str.append("Configuration:\n").append(config.toString());
    str.append("Test user ID = ").append(authenticator.getUserInfo("myName"));
    return str.toString();
  }
}
