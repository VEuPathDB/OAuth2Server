package org.gusdb.oauth2.service;

import static org.gusdb.oauth2.assets.StaticResource.RESOURCE_PREFIX;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;
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

  // supported paths
  private static final String LOGIN_PATH = "login";
  private static final String LOGOUT_PATH = "logout";
  private static final String AUTHORIZATION_PATH = "authorize";
  private static final String TOKEN_PATH = "token";
  private static final String USER_INFO_PATH = "user";
  private static final String CHANGE_PASSWORD_PATH = "changePassword";
  private static final String DISCOVERY_PATH = "discovery";
  private static final String JWKS_PATH = "jwks";

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
    LOG.trace("Request made to fetch resource: " + name);
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
  @Path(LOGIN_PATH)
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
        LOG.warn("Attempt to access resources with form ID " + formId +
            " that does not match formId in session (any of " + concat(session.getFormIds()));
        return Response.seeOther(getLoginUri(formId, "", LoginFormStatus.accessdenied)).build();
      }
      Authenticator authenticator = OAuthServlet.getAuthenticator(_context);
      boolean validCreds = authenticator.isCredentialsValid(username, password);
      if (validCreds) {
        LOG.info("Authentication successful.  Setting session username to " + username);
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
        return Response.seeOther(getLoginUri(formId,
            session.getOriginalRequest(formId).getRedirectUri(),
            LoginFormStatus.failed)).build();
      }
    }
    catch (Exception e) {
      LOG.error("Error processing /login request", e);
      return Response.seeOther(getLoginUri(formId, "", LoginFormStatus.error)).build();
    }
  }

  private String concat(Set<String> strs) {
    StringBuilder sb = new StringBuilder("[ ");
    for (String s : strs) { sb.append(s).append(", "); }
    return sb.append("<END>").toString().replace(", <END>", " ]");
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
  @Path(LOGOUT_PATH)
  public Response logOut(@QueryParam("redirect_uri") String redirectUri) {
    // FIXME: cannot get CORS requests to work so logout can be async from a different domain
    // determine whether this request came from a valid page
    try {
      @SuppressWarnings("unused")
      URL url = new URL(redirectUri);
      //String passedPort = (url.getPort() == -1 ? "" : ":" + url.getPort());
      //String originVal = url.getProtocol() + "://" + url.getHost() + passedPort;
      //ClientValidator clientValidator = OAuthServlet.getClientValidator(_context);
      //if (!clientValidator.isValidLogoutClient(redirectUri)) {
      //  return Response
      //      .status(Status.FORBIDDEN)
      //      .header("Access-Control-Allow-Origin", originVal)
      //      .entity("Valid client redirect URI required")
      //      .build();
      //}

      // invalidate the session
      new Session(_request.getSession()).invalidate();

      // attempt to construct redirect response; if unable, just return 200
      return Response
          .seeOther(new URI(redirectUri))
          //.header("Access-Control-Allow-Origin", originVal)
          .build();
    }
    catch (MalformedURLException | URISyntaxException e) {
      //return new OAuthResponseFactory().buildBadRedirectUrlResponse();
      return Response.ok("Logged out at " + new Date()).build();
    }
  }

  @GET
  @Path(AUTHORIZATION_PATH)
  public Response authorize() throws URISyntaxException, OAuthSystemException {
    try {
     LOG.info("Handling authorize request with the following params:" +
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
        AuthzRequest request = new AuthzRequest(oauthRequest);
        return Response.seeOther(getLoginUri(
            session.generateFormId(request),
            request.getRedirectUri(),
            null)).build();
      }
    }
    catch (OAuthProblemException e) {
      LOG.error("Problem with authorize request: ", e);
      return new OAuthResponseFactory().buildInvalidRequestResponse(e);
    }
  }

  private URI getLoginUri(String formId, String redirectUri, LoginFormStatus status) throws URISyntaxException {
    String queryString = "";
    if (formId != null) {
      queryString = FORM_ID_PARAM_NAME + "=" + formId;
    }
    if (redirectUri != null) {
      queryString += (queryString.isEmpty() ? "" : "&") + "redirectUri=" + encodeUrl(redirectUri);
    }
    if (status != null) {
      queryString += (queryString.isEmpty() ? "" : "&") + "status=" + status.name();
    }
    return new URI(RESOURCE_PREFIX + OAuthServlet.getApplicationConfig(_context).getLoginFormPage() +
        (queryString.isEmpty() ? "" : "?" + queryString));
  }

  private static String encodeUrl(String redirectUri) {
    try {
      return URLEncoder.encode(redirectUri, "UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException("UTF-8 encoding is no longer supported.", e);
    }
  }

  @POST
  @Path(TOKEN_PATH)
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getToken(MultivaluedMap<String, String> formParams) throws OAuthSystemException {
    try {
      // for POST + URL-encoded form, must use custom HttpServletRequest with Jersey to read actual params
      HttpServletRequest request = new JerseyHttpRequestWrapper(_request, formParams);
      LOG.info("Handling token request with the following params:" +
        System.lineSeparator() + paramsToString(request));
      OAuthTokenRequest oauthRequest = new OAuthTokenRequest(request);
      ClientValidator clientValidator = OAuthServlet.getClientValidator(_context);
      if (!clientValidator.isValidTokenClient(oauthRequest)) {
        return new OAuthResponseFactory().buildInvalidClientResponse();
      }
      ApplicationConfig config = OAuthServlet.getApplicationConfig(_context);
      return OAuthRequestHandler.handleTokenRequest(oauthRequest,
          OAuthServlet.getAuthenticator(_context), config);
    }
    catch (OAuthProblemException e) {
      LOG.error("Problem with authorize request: ", e);
      return new OAuthResponseFactory().buildInvalidRequestResponse(e);
    }
  }

  private static String paramsToString(HttpServletRequest request) {
    Map<String, String[]> params = request.getParameterMap();
    StringBuilder sb = new StringBuilder("{").append(System.lineSeparator());
    for (String key : params.keySet()) {
      String[] values = (key.equals("client_secret") ? new String[]{ "<blocked>" } : params.get(key));
      sb.append("  ").append(key).append(": ")
        .append(Arrays.toString(values)).append(System.lineSeparator());
    }
    return sb.append("}").toString();
  }

  @GET
  @Path(USER_INFO_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserInfo() throws Exception {
    ApplicationConfig config = OAuthServlet.getApplicationConfig(_context);
    OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(_request, ParameterStyle.HEADER);
    return OAuthRequestHandler.handleUserInfoRequest(oauthRequest, OAuthServlet.getAuthenticator(_context), config.getIssuer(), config.getTokenExpirationSecs());
  }

  @POST
  @Path(CHANGE_PASSWORD_PATH)
  public Response changePassword(String body) {

    // parse request params
    String username, password, newPassword;
    try {
      LOG.debug("Received body from client: " + body);
      JsonObject input = Json.createReader(new StringReader(body)).readObject();
      username = input.getString("username");
      password = input.getString("password");
      newPassword = input.getString("newPassword");
    }
    catch (NullPointerException | ClassCastException | JsonParsingException e) {
      return Response.status(Status.BAD_REQUEST).build();
    }

    // request properly formatted, check if credentials are valid
    Authenticator auth = OAuthServlet.getAuthenticator(_context);
    try {
      LOG.debug("Trying creds: " + username + "/" + password);
      if (!auth.isCredentialsValid(username, password)) {
        // wrong password given for the passed user
        return Response.status(Status.FORBIDDEN).build();
      }
      // credentials valid; overwrite password
      LOG.debug("Overwriting password: " + username + "/" + password);
      auth.overwritePassword(username, newPassword);
      return Response.ok().build();
    }
    catch (Exception e) {
      LOG.error("Error while processing password change request", e);
      return Response.serverError().build();
    }
  }

  @GET
  @Path(DISCOVERY_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getConfiguration() {
    String baseUrl = _request.getRequestURL().toString();
    baseUrl = baseUrl.substring(0, baseUrl.indexOf(DISCOVERY_PATH));
    JsonArray responseTypes = buildArray("code", "id_token");
    JsonArray grantTypes = buildArray("authorization_code");
    JsonArray subjectTypes = buildArray("public");
    JsonArray supportedAlgorithms = buildArray("HS512");
    JsonArray claims = buildArray(IdTokenFactory.IdTokenFields
        .getNames().toArray(new String[0]));
    return Response.ok(
      OAuthRequestHandler.prettyPrintJsonObject(
        Json.createObjectBuilder()
          .add("issuer", baseUrl)
          .add("authorization_endpoint", baseUrl + AUTHORIZATION_PATH)
          .add("token_endpoint", baseUrl + TOKEN_PATH)
          .add("userinfo_endpoint", baseUrl + USER_INFO_PATH)
          .add("jwks_uri", baseUrl + JWKS_PATH)
          .add("response_types_supported", responseTypes)
          .add("grant_types_supported", grantTypes)
          .add("subject_types_supported", subjectTypes)
          .add("id_token_signing_alg_values_supported", supportedAlgorithms)
          .add("claims_supported", claims)
          .build()
          .toString()
        )
      ).build();
  }

  private JsonArray buildArray(String... strings) {
    JsonArrayBuilder builder = Json.createArrayBuilder();
    for (String str : strings) {
      builder.add(str);
    }
    return builder.build();
  }

  @GET
  @Path(JWKS_PATH)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getJwksJson() {
    return Response.ok(
      OAuthRequestHandler.prettyPrintJsonObject(
        Json.createObjectBuilder()
          .add("keys", Json.createArrayBuilder()
            .add(Json.createObjectBuilder()
              .add("kty", "RSA")
              .add("n", "<your_client_secret>")
              .add("alg", "HS512")
              .add("kid", "0")
              .build())
            .build())
          .build()
          .toString()))
          .build();
  }
}
