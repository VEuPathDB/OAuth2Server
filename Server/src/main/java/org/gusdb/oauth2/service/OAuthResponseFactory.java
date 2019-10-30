package org.gusdb.oauth2.service;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

import org.apache.oltu.oauth2.common.exception.OAuthProblemException;

public class OAuthResponseFactory {

  private static ResponseBuilder NOT_ACCEPTABLE_RESPONSE = Response.notAcceptable(Collections.<Variant>emptyList());

  public Response buildInvalidGrantTypeResponse() {
    return getNotAcceptableResponse("Unsupported Grant Type");
  }

  public Response buildInvalidUserPassResponse() {
    return getNotAcceptableResponse("Invalid Username/Password");
  }

  public Response buildInvalidClientResponse() {
    return getNotAcceptableResponse("Invalid Client");
  }

  public Response buildBadAuthCodeResponse() {
    return getNotAcceptableResponse("Invalid Auth Code");
  }

  public Response buildBadResponseTypeResponse() {
    return getNotAcceptableResponse("Unsupported Response Type");
  }

  public Response buildBadRedirectUrlResponse() {
    return getNotAcceptableResponse("Invalid Redirect URI in Request");
  }

  public Response buildInvalidRequestResponse(OAuthProblemException e) {
    return getNotAcceptableResponse(e.getMessage());
  }

  private static Response getNotAcceptableResponse(String message) {
    return NOT_ACCEPTABLE_RESPONSE.entity(message).build();
  }

  public Response buildServerErrorResponse() {
    return Response.serverError().build();
  }

}
