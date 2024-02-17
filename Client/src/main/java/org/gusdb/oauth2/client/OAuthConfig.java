package org.gusdb.oauth2.client;

/**
 * Encapsulates all the configuration values a client may need to
 * perform OAuth 2.0 functions e.g. token requests
 *
 * @author rdoherty
 */
public interface OAuthConfig {

  /**
   * Creates an instance of OAuthConfig from the passed values
   */
  public static OAuthConfig build(String oauthUrl, String oauthClientId, String oauthClientSecret) {
    return new OAuthConfig() {
      @Override public String getOauthUrl()          { return oauthUrl;          }
      @Override public String getOauthClientId()     { return oauthClientId;     }
      @Override public String getOauthClientSecret() { return oauthClientSecret; }
    };
  }

  /**
   * @return base URL of OAuth2 server to use for authentication
   */
  String getOauthUrl();

  /**
   * @return OAuth2 client ID to use for authentication
   */
  String getOauthClientId();

  /**
   * @return OAuth2 client secret to use for authentication
   */
  String getOauthClientSecret();

}
