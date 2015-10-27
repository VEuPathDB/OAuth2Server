package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map.Entry;

import javax.json.JsonObject;
import javax.json.JsonValue;
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

    // build response according to response_type
    String responseType = oauthRequest.getResponseType();
    LOG.trace("Cached request values: " + responseType + ", " + oauthRequest.getClientId() + ", " +
        oauthRequest.getRedirectUri() + ", " + oauthRequest.getResponseType());

    LOG.debug("Creating authorization response");
    OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(
        new StateParamHttpRequest(oauthRequest.getState()), HttpServletResponse.SC_FOUND);

    if (responseType.equals(ResponseType.CODE.toString())) {
      LOG.debug("Generating authorization code...");
      final String authorizationCode = oauthIssuerImpl.authorizationCode();
      TokenStore.addAuthCode(new AuthCodeData(authorizationCode, oauthRequest.getClientId(), username));
      builder.setCode(authorizationCode);
    }
    else {
      return new OAuthResponseFactory().buildBadResponseTypeResponse();
    }

    String redirectURI = oauthRequest.getRedirectUri();
    final OAuthResponse response = builder.location(redirectURI).setExpiresIn(String.valueOf(expirationSecs)).buildQueryMessage();
    URI url = new URI(response.getLocationUri());
    return Response.status(response.getResponseStatus()).location(url).build();
  }

  public static Response handleTokenRequest(OAuthTokenRequest oauthRequest,
      Authenticator authenticator, int expirationSecs, boolean includeUserInfo) throws OAuthSystemException {
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
          // Don't current support password grant
          return responses.buildInvalidGrantTypeResponse();
          /*
          try {
            if (!authenticator.isCredentialsValid(oauthRequest.getUsername(), oauthRequest.getPassword())) {
              return responses.buildInvalidUserPassResponse();
            }
          }
          catch (Exception e) {
            return responses.buildServerErrorResponse();
          }
          break;
          */
        case REFRESH_TOKEN:
          // refresh token is not supported in this implementation
          return responses.buildInvalidGrantTypeResponse();
        default:
          return responses.buildInvalidGrantTypeResponse();
      }

      final String accessToken = oauthIssuerImpl.accessToken();
      TokenStore.addAccessToken(accessToken, oauthRequest.getCode());

      OAuthTokenResponseBuilder responseBuilder = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK).setAccessToken(
          accessToken).setExpiresIn(String.valueOf(expirationSecs));

      if (includeUserInfo) {
        JsonObject userObj = getUserInfo(authenticator, TokenStore.getUserForToken(accessToken));
        for (Entry<String, JsonValue> entry : userObj.entrySet()) {
          switch (entry.getKey()) {
            case OAuth.OAUTH_ACCESS_TOKEN:
            case OAuth.OAUTH_EXPIRES_IN:
            case OAuth.OAUTH_REFRESH_TOKEN:
            case OAuth.OAUTH_TOKEN_TYPE:
              LOG.warn("Authenticator tried to override standard token response property '" + entry.getKey() + "'; skipping");
              break;
            default:
              responseBuilder.setParam(entry.getKey(), getUserInfoPropertyString(entry.getValue()));
          }
        }
      }

      OAuthResponse response = responseBuilder.buildJSONMessage();
      return Response.status(response.getResponseStatus()).entity(response.getBody()).build();
    }
    catch (OAuthProblemException e) {
      LOG.error("Problem responding to token request", e);
      OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e).buildJSONMessage();
      return Response.status(res.getResponseStatus()).entity(res.getBody()).build();
    }
  }

  private static String getUserInfoPropertyString(JsonValue value) {
    switch (value.getValueType()) {
      case OBJECT:
        return "[Object]";
      case ARRAY:
        return "[Array]";
      default:
        return value.toString();
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
      Authenticator authenticator) throws OAuthSystemException {
    String accessToken = oauthRequest.getAccessToken();
    String username = TokenStore.getUserForToken(accessToken);

    // Validate the access token
    if (username == null) {
      // Return the OAuth error message
      OAuthResponse oauthResponse = OAuthRSResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED).setError(
          OAuthError.ResourceResponse.INVALID_TOKEN).buildHeaderMessage();

      // return Response.status(Response.Status.UNAUTHORIZED).build();
      return Response.status(Response.Status.UNAUTHORIZED).header(OAuth.HeaderType.WWW_AUTHENTICATE,
          oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE)).build();
    }

    JsonObject userData = getUserInfo(authenticator, username);
    return Response.status(Response.Status.OK).entity(userData.toString()).build();
  }

  private static JsonObject getUserInfo(Authenticator authenticator, String username) throws OAuthSystemException {
    try {
      return authenticator.getUserInfo(username);
    }
    catch (Exception e) {
      LOG.error("Unable to retrieve user info for usernaem '" + username + "'", e);
      throw new OAuthSystemException(e);
    }
  }
}
