package org.gusdb.oauth2.service;

import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.stream.JsonGenerator;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.as.response.OAuthASResponse.OAuthTokenResponseBuilder;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.service.TokenStore.AccessTokenData;
import org.gusdb.oauth2.service.TokenStore.AuthCodeData;
import org.gusdb.oauth2.service.util.AuthzRequest;
import org.gusdb.oauth2.service.util.StateParamHttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthRequestHandler.class);

  public static Response handleAuthorizationRequest(AuthzRequest oauthRequest, String username, int expirationSecs)
      throws URISyntaxException, OAuthSystemException {
    OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());

    // 'code' is the only response type supported
    String responseType = oauthRequest.getResponseType();
    if (!responseType.equals(ResponseType.CODE.toString())) {
      return new OAuthResponseFactory().buildBadResponseTypeResponse();
    }

    LOG.trace("Cached request values: " + responseType + ", " + oauthRequest.getClientId() + ", " +
        oauthRequest.getRedirectUri() + ", " + oauthRequest.getResponseType());

    LOG.debug("Creating authorization response");
    OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(
        new StateParamHttpRequest(oauthRequest.getState()), HttpServletResponse.SC_FOUND);

    LOG.debug("Generating authorization code...");
    final String authorizationCode = oauthIssuerImpl.authorizationCode();
    TokenStore.addAuthCode(new AuthCodeData(authorizationCode,
        oauthRequest.getClientId(), username, oauthRequest.getNonce()));
    builder.setCode(authorizationCode);

    String redirectURI = oauthRequest.getRedirectUri();
    final OAuthResponse response = builder.location(redirectURI)
        .setExpiresIn(String.valueOf(expirationSecs)).buildQueryMessage();
    URI url = new URI(response.getLocationUri());
    return Response.status(response.getResponseStatus()).location(url).build();
  }

  public static Response handleTokenRequest(OAuthTokenRequest oauthRequest,
      Authenticator authenticator, ApplicationConfig config) throws OAuthSystemException {
    int expirationSecs = config.getTokenExpirationSecs();
    boolean isOpenIdConnect = config.useOpenIdConnect();
    try {
      OAuthResponseFactory responses = new OAuthResponseFactory();
      OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
      // do checking for different grant types
      GrantType grantType = getGrantType(oauthRequest);
      switch (grantType) {
        case AUTHORIZATION_CODE:
          if (!TokenStore.isValidAuthCode(oauthRequest.getCode(), oauthRequest.getClientId())) {
            LOG.info("Returning bad auth code response; could not find code " +
                oauthRequest.getCode() + " for client " + oauthRequest.getClientId());
            return responses.buildBadAuthCodeResponse();
          }
          break;
        case PASSWORD:
          // don't currently support password grant
          return responses.buildInvalidGrantTypeResponse();
        case REFRESH_TOKEN:
          // refresh token is not supported in this implementation
          return responses.buildInvalidGrantTypeResponse();
        default:
          return responses.buildInvalidGrantTypeResponse();
      }

      final String accessToken = oauthIssuerImpl.accessToken();
      AccessTokenData tokenData = TokenStore.addAccessToken(accessToken, oauthRequest.getCode());

      OAuthTokenResponseBuilder responseBuilder =
          OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
          .setTokenType("Bearer")
          .setAccessToken(accessToken)
          .setExpiresIn(String.valueOf(expirationSecs));

      // if configured to send id_token with access token response, create and add it
      if (isOpenIdConnect) {
        responseBuilder.setParam("id_token", IdTokenFactory.createJwtFromJson(
            IdTokenFactory.createIdTokenJson(authenticator, tokenData, config.getIssuer(), expirationSecs),
            config.getSecretMap().get(tokenData.authCodeData.clientId)));
      }

      OAuthResponse response = responseBuilder.buildJSONMessage();
      if (LOG.isDebugEnabled()) {
        LOG.debug("Processed token request successfully.  Returning: " + prettyPrintJsonObject(response.getBody()));
      }
      return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
    }
    catch (OAuthProblemException e) {
      LOG.error("Problem responding to token request", e);
      OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e).buildJSONMessage();
      return Response.status(res.getResponseStatus()).entity(res.getBody()).build();
    }
  }

  private static GrantType getGrantType(OAuthTokenRequest oauthRequest) throws OAuthProblemException {
    String grantTypeStr = oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE);
    try {
      return GrantType.valueOf(grantTypeStr.toUpperCase());
    }
    catch (IllegalArgumentException e) {
      throw OAuthProblemException.error(OAuthError.TokenResponse.INVALID_REQUEST, "Illegal grant type: " + grantTypeStr);
    }
  }

  public static Response handleUserInfoRequest(OAuthAccessResourceRequest oauthRequest,
      Authenticator authenticator, String issuer, int expirationSecs)
          throws OAuthSystemException, OAuthProblemException {
    String accessToken = oauthRequest.getAccessToken();
    AccessTokenData tokenData = TokenStore.getTokenData(accessToken);

    // Validate the access token
    if (tokenData == null) {
      // Return the OAuth error message
      OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED).setError(
          OAuthError.ResourceResponse.INVALID_TOKEN).buildHeaderMessage();

      // return Response.status(Response.Status.UNAUTHORIZED).build();
      return Response.status(Response.Status.UNAUTHORIZED).header(OAuth.HeaderType.WWW_AUTHENTICATE,
          oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE)).build();
    }

    JsonObject idTokenData = IdTokenFactory.createIdTokenJson(authenticator, tokenData, issuer, expirationSecs);
    return Response.status(Response.Status.OK).entity(idTokenData.toString()).build();
  }

  public static String prettyPrintJsonObject(String json) {
    JsonObject obj = Json.createReader(new StringReader(json)).readObject();
    StringWriter stringWriter = new StringWriter();
    Map<String, Object> properties = new HashMap<String, Object>(1);
    properties.put(JsonGenerator.PRETTY_PRINTING, true);
    Json.createWriterFactory(properties).createWriter(stringWriter).writeObject(obj);
    return stringWriter.toString();
  }
}
