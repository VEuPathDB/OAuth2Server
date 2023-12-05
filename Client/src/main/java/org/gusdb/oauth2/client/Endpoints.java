package org.gusdb.oauth2.client;

public class Endpoints {

  // public endpoints supported by the OAuth/OIDC service
  public static final String /* GET   */ DISCOVERY      = "/discovery";
  public static final String /* GET   */ JWKS           = "/jwks";
  public static final String /* GET   */ ASSETS         = "/assets/";
  public static final String /* POST  */ LOGIN          = "/login";          // submission from HTML form
  public static final String /* POST  */ USER_CHANGE_PW = "/changePassword"; // submission from HTML form
  public static final String /* GET   */ LOGOUT         = "/logout";
  public static final String /* GET   */ AUTHORIZE      = "/authorize";
  public static final String /* POST  */ AUTH_TOKEN     = "/token";          // requires client secret + (auth code or username/password)
  public static final String /* POST  */ BEARER_TOKEN   = "/bearer-token";   // requires client secret + (auth code or username/password)
  public static final String /* POST  */ GUEST_TOKEN    = "/guest-token";    // requires client secret
  public static final String /* POST  */ QUERY_USERS    = "/query";          // requires client secret
  public static final String /* POST  */ USER_CREATE    = "/user";           // requires client secret
  public static final String /* GET   */ USER_INFO      = "/user";           // requires bearer token
  public static final String /* PATCH */ USER_EDIT      = "/user";           // requires bearer token

  // static class
  private Endpoints() {}

}
