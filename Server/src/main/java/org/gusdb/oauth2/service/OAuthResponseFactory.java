package org.gusdb.oauth2.service;

import java.util.Collections;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

public class OAuthResponseFactory {

  private static ResponseBuilder NOT_ACCEPTABLE_RESPONSE = Response.notAcceptable(Collections.<Variant>emptyList());

  public Response buildInvalidUserPassResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Username/Password").build();
  }

  public Response buildInvalidClientSecretResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Client Secret").build();
  }

  public Response buildBadAuthCodeResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Auth Code").build();
  }

  public Response buildInvalidClientIdResponse() {
    return NOT_ACCEPTABLE_RESPONSE.entity("Invalid Client ID").build();
  }

  public Response buildServerErrorResponse() {
    return Response.serverError().build();
  }

}
