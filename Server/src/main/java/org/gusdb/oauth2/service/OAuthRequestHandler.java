package org.gusdb.oauth2.service;

import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
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
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.config.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OAuthRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(OAuthRequestHandler.class);
  
  public static Response handleAuthorizationRequest(OAuthAuthzRequest oauthRequest, String username)
      throws URISyntaxException, OAuthSystemException {
      OAuthIssuerImpl oauthIssuerImpl =
          new OAuthIssuerImpl(new MD5Generator());

      // build response according to response_type
      String responseType =
          oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);

      LOG.info("Creating authorization response");
      OAuthASResponse.OAuthAuthorizationResponseBuilder builder =
          OAuthASResponse.authorizationResponse(new StateParamHttpRequest(oauthRequest.getState()),
              HttpServletResponse.SC_FOUND);

      // 1
      LOG.info("Checking if requested response_type is '" + ResponseType.CODE.toString() + "'");
      if (responseType.equals(ResponseType.CODE.toString())) {
        LOG.info("Generating authorization code...");
        final String authorizationCode =
            oauthIssuerImpl.authorizationCode();
        StateCache.addAuthCode(authorizationCode);
        builder.setCode(authorizationCode);
      }

      String redirectURI =
          oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI);
      final OAuthResponse response = builder
          .location(redirectURI)
          .buildQueryMessage();
      URI url = new URI(response.getLocationUri());
      return Response.status(response.getResponseStatus())
          .location(url)
          .build();
  }

  public static Response handleTokenRequest(HttpServletRequest request,
      ApplicationConfig config, Authenticator authenticator, OAuthResponseFactory responses) throws OAuthSystemException {
    try {
      OAuthTokenRequest oauthRequest =
          new OAuthTokenRequest(request);
      OAuthIssuer oauthIssuerImpl =
          new OAuthIssuerImpl(new MD5Generator());
      ClientValidator clientValidator =
          new ClientValidator(config.getAllowedClients());

      // check if clientid is valid
      if (!clientValidator.checkClientId(oauthRequest.getClientId())) {
        return responses.buildInvalidClientIdResponse();
      }

      // check if client_secret is valid
      if (!clientValidator.checkClientSecret(oauthRequest.getClientId(), oauthRequest.getClientSecret())) {
        return responses.buildInvalidClientSecretResponse();
      }

      // do checking for different grant types
      if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE)
          .equals(GrantType.AUTHORIZATION_CODE.toString())) {
        if (!StateCache.checkAuthCode(oauthRequest.getParam(OAuth.OAUTH_CODE))) {
          return responses.buildBadAuthCodeResponse();
        }
      } else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE)
          .equals(GrantType.PASSWORD.toString())) {
        try {
          if (!authenticator.isCredentialsValid(oauthRequest.getUsername(),
              oauthRequest.getPassword())) {
            return responses.buildInvalidUserPassResponse();
          }
        } catch (Exception e) {
          return responses.buildServerErrorResponse();
        }
      } else if (oauthRequest.getParam(OAuth.OAUTH_GRANT_TYPE)
          .equals(GrantType.REFRESH_TOKEN.toString())) {
        // refresh token is not supported in this implementation
        return responses.buildInvalidUserPassResponse();
      }

      final String accessToken = oauthIssuerImpl.accessToken();
      StateCache.addToken(accessToken);

      OAuthResponse response = OAuthASResponse
          .tokenResponse(HttpServletResponse.SC_OK)
          .setAccessToken(accessToken)
          .setExpiresIn("3600")
          .buildJSONMessage();
      return Response.status(response.getResponseStatus())
          .entity(response.getBody()).build();

    }
    catch (OAuthProblemException e) {
      LOG.error("Problem responding to token request", e);
      OAuthResponse res = OAuthASResponse
          .errorResponse(HttpServletResponse.SC_BAD_REQUEST)
          .error(e)
          .buildJSONMessage();
      return Response
          .status(res.getResponseStatus()).entity(res.getBody())
          .build();
    }
    // ...
  }

  public static Response handleUserInfoRequest(HttpServletRequest request) throws OAuthSystemException {
    try {
      // Make the OAuth Request out of this request
      OAuthAccessResourceRequest oauthRequest =
          new OAuthAccessResourceRequest(request, ParameterStyle.HEADER);
      // Get the access token
      String accessToken = oauthRequest.getAccessToken();

      // Validate the access token
      if (!StateCache.isValidToken(accessToken)) {
        // Return the OAuth error message
        OAuthResponse oauthResponse = OAuthRSResponse
            .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
            .setError(OAuthError.ResourceResponse.INVALID_TOKEN)
            .buildHeaderMessage();

        //return Response.status(Response.Status.UNAUTHORIZED).build();
        return Response.status(Response.Status.UNAUTHORIZED)
            .header(OAuth.HeaderType.WWW_AUTHENTICATE,
                oauthResponse
                .getHeader(OAuth.HeaderType.WWW_AUTHENTICATE))
                .build();

      }
      // [1]
      return Response.status(Response.Status.OK)
          .entity(accessToken).build();
    }
    catch (OAuthProblemException e) {
      LOG.error("Problem responding to user info request", e);
      return Response.serverError().build();
    }
  }
}
