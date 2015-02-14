package org.gusdb.oauth2.service;


public class OAuthValidationAction {
/*
  @Override
  protected ActionResult handleRequest(ParamGroup params) throws Exception {
    HttpServletRequest request = ((HttpRequestData)getRequestData()).getUnderlyingRequest();
    //return handleAuthRequest1(request);
    return handleAuthRequest2(request);
  }

  private ActionResult handleAuthRequest2(HttpServletRequest request) throws OAuthSystemException {
    OAuthResponse response;
    try {
      OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);
      OAuthIssuerImpl oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());

      //build response according to response_type
      String responseType = oauthRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);

      OAuthASResponse.OAuthAuthorizationResponseBuilder builder =
          OAuthASResponse.authorizationResponse(request,
              HttpServletResponse.SC_FOUND);

      // 1
      if (responseType.equals(ResponseType.CODE.toString())) {
        final String authorizationCode =
            oauthIssuerImpl.authorizationCode();
        //database.addAuthCode(authorizationCode);
        builder.setCode(authorizationCode);
      }

      String redirectURI =
          oauthRequest.getParam(OAuth.OAUTH_REDIRECT_URI);
      response = builder
          .location(redirectURI)
          .buildQueryMessage();
    }
    catch(OAuthProblemException ex) {
      response = OAuthASResponse
          .errorResponse(HttpServletResponse.SC_FOUND)
          .error(ex)
          .location(ex.getRedirectUri())
          .buildQueryMessage();
    }

    return new ActionResult()
      .setHttpResponseStatus(response.getResponseStatus())
      .setExternalPath(response.getLocationUri());
  }

  private ActionResult handleAuthRequest1(HttpServletRequest request) throws OAuthSystemException {
    OAuthResponse response;
    try {
      //dynamically recognize an OAuth profile based on request characteristic (params,
      // method, content type etc.), perform validation
      OAuthAuthzRequest oauthRequest = new OAuthAuthzRequest(request);

      // if user is already logged in, then 
      new OAuthCredentialValidator(getWdkModel().getModel().getUserFactory())
          .validateRedirectionURI(oauthRequest);
      
      //build OAuth response
      response = OAuthASResponse
          .authorizationResponse(request, HttpServletResponse.SC_FOUND)
          .setCode(new OAuthIssuerImpl(new MD5Generator()).authorizationCode())
          .location(oauthRequest.getRedirectURI())
          .buildQueryMessage();
    }
    catch(OAuthProblemException ex) {
      response = OAuthASResponse
          .errorResponse(HttpServletResponse.SC_FOUND)
          .error(ex)
          .location(ex.getRedirectUri())
          .buildQueryMessage();
    }
    
    return new ActionResult().setExternalPath(response.getLocationUri());
  }
  */
}
