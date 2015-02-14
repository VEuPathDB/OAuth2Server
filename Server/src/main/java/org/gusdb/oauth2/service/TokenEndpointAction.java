package org.gusdb.oauth2.service;


public class TokenEndpointAction {
/*
  @Override
  protected ActionResult handleRequest(ParamGroup params) throws Exception {

    OAuthTokenRequest oauthRequest = null;

    OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());

    OAuthResponse oauthResponse;
    
    try {
      oauthRequest = new OAuthTokenRequest(((HttpRequestData)getRequestData()).getUnderlyingRequest());

      new OAuthCredentialValidator(getWdkModel().getModel().getUserFactory()).validateClient(oauthRequest);

      //String authzCode = oauthRequest.getCode();

      // some code

      String accessToken = oauthIssuerImpl.accessToken();
      String refreshToken = oauthIssuerImpl.refreshToken();

      // some code

      oauthResponse = OAuthASResponse
          .tokenResponse(HttpServletResponse.SC_OK)
          .setAccessToken(accessToken)
          .setExpiresIn("3600")
          .setRefreshToken(refreshToken)
          .buildJSONMessage();
    }
    catch(OAuthProblemException ex) {
      oauthResponse = OAuthResponse
          .errorResponse(401)
          .error(ex)
          .buildJSONMessage();
    }

    return new ActionResult(ResponseType.json)
        .setHttpResponseStatus(oauthResponse.getResponseStatus())
        .setStream(IoUtil.getStreamFromString(oauthResponse.getBody()));
  }
  */
}
