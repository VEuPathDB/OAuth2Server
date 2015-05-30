package org.gusdb.oauth2.service;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

public class OAuthResponseFactory {

  private static ResponseBuilder NOT_ACCEPTABLE_RESPONSE = Response.notAcceptable(Collections.<Variant>emptyList());

  public Response buildInvalidGrantTypeResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Unsupported Grant Type").build();
  }

  public Response buildInvalidUserPassResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Username/Password").build();
  }

  public Response buildInvalidClientResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Client").build();
  }

  public Response buildBadAuthCodeResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Auth Code").build();
  }

  public Response buildBadResponseTypeResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Unsupported Response Type").build();
  }

  public Response buildBadRedirectUrlResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Redirect URI in Request").build();
  }

  public Response buildServerErrorResponse() {
    return Response.serverError().build();
  }

}
