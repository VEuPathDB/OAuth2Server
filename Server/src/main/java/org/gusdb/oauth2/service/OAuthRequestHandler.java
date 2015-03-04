package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;

import javax.json.JsonObject;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;

import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthRequestHandler.class);

  public static Response handleAuthorizationRequest(OAuthAuthzRequest oauthRequest, String username)
      throws URISyntaxException, OAuthSystemException {
    OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());

    // build response according to response_type
    String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);

    LOG.info("Creating authorization response");
    OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(
        new StateParamHttpRequest(oauthRequest.getState()), HttpServletResponse.SC_FOUND);

    // 1
    LOG.info("Checking if requested response_type is '" + ResponseType.CODE.toString() + "'");
    if (responseType.equals(ResponseType.CODE.toString())) {
      LOG.info("Generating authorization code...");
      final String authorizationCode = oauthIssuerImpl.authorizationCode();
      TokenStore.addAuthCode(new AuthCodeData(authorizationCode, oauthRequest.getClientId(), username));
      builder.setCode(authorizationCode);
    }

    String redirectURI = oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI);
    final OAuthResponse response = builder.location(redirectURI).buildQueryMessage();
    URI url = new URI(response.getLocationUri());
    return Response.status(response.getResponseStatus()).location(url).build();
  }

  public static Response handleTokenRequest(OAuthTokenRequest oauthRequest,
      Authenticator authenticator) throws OAuthSystemException {
    try {
      OAuthResponseFactory responses = new OAuthResponseFactory();
      OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
      // do checking for different grant types
      GrantType grantType = getGrantType(oauthRequest);
      switch (grantType) {
        case AUTHORIZATION_CODE:
          if (!TokenStore.isValidAuthCode(oauthRequest.getCode(), oauthRequest.getClientId())) {
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
      TokenStore.addAccessToken(oauthRequest.getCode(), accessToken);

      OAuthResponse response = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK).setAccessToken(
          accessToken).setExpiresIn("3600").buildJSONMessage();
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
      throw OAuthProblemException.error("invalid_request", "Illegal grant type: " + grantTypeStr);
    }
  }

  public static Response handleUserInfoRequest(OAuthAccessResourceRequest oauthRequest, Authenticator authenticator) {
    try {
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

      JsonObject userData = authenticator.getUserInfo(username);
      return Response.status(Response.Status.OK).entity(userData).build();
    }
    catch (Exception e) {
      LOG.error("Problem responding to user info request", e);
      return Response.serverError().build();
    }
  }
}
