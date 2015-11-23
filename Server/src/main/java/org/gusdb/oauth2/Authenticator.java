package org.gusdb.oauth2;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

/**
 * Provides access to a custom user store to authenticate users and provide
 * basic user information to approved clients.  Users of the OAuth2 server
 * must provide an implementation of this class specific to their user storage
 * mechanism.
 * 
 * @author rdoherty
 */
public interface Authenticator {

  /**
   * Provides access to user's ID and email
   * 
   * @author rdoherty
   */
  public static interface UserInfo {
    /**
     * Returns system-unique and static ID for a user; this identifier should be
     * suitable for representing a primary key for this user.  It will be
     * used as the "sub" property of the OpenID Connect ID token.  If null
     * or an empty value is returned, and exception will be thrown.
     * 
     * @return unique user ID
     */
    public String getUserId();

    /**
     * Returns user's email address; if this method returns a value, it will be
     * used as the "email" property of the OpenID Connect ID token.  If null
     * or an empty String is returned, the "email" property will be omitted.
     * 
     * @return user's email address
     */
    public String getEmail();

    /**
     * Returns whether this user's email has been verified.  The value returned
     * will be used as the "email_verified" property of the OpenID Connect ID
     * token.  This will only be included, however, if the "email" property is
     * also included.
     * 
     * @return whether the user's email address has been verified
     */
    public boolean isEmailVerified();

    /**
     * Returns supplemental fields to be included in the OpenID Connect ID
     * token.  Keys cannot override natively-supported fields, but any other
     * value is valid.  Any JSON value is allowed.
     * 
     * @return supplemental fields to add to ID token
     */
    public Map<String,JsonValue> getSupplementalFields();
  }

  /**
   * Initializes this authenticator using configuration set in the OAuth config
   * file.  If this method opens non-transient resources (that last the life
   * of the Authenticator), the org.gusdb.oauth2.server.ApplicationListener
   * should be added to web.xml so that close() is called on webapp unload.
   * 
   * @param configJson configuration for this authenticator
   * @throws InitializationException if passed JSON is malformed or incomplete,
   * or if an error occurs during initialization
   */
  public void initialize(JsonObject configJson) throws InitializationException;

  /**
   * Replies true if the passed credentials identify a valid user, else false.
   * The passed strings are not checked for SQL-injection or other hacks.
   * 
   * @param username entered username
   * @param password entered password
   * @return true if passed creds identify a user, else false
   * @throws Exception if something goes wrong during validation
   */
  public boolean isCredentialsValid(String username, String password) throws Exception;

  /**
   * Returns implementation-specific user information.  Only the user ID field
   * of the returned object is required.  Email and EmailVerified are optional.
   * Authenticator implementations can also add supplemental fields in the
   * supplementalFields property of the returned object.
   * 
   * This information is used to populate an OpenID Connect ID token, which is
   * returned for /token requests if the "includeUserInfoWithToken" config value
   * is true, and is returned for /user requests accompanied by a valid access
   * token.
   * 
   * @param username username for which to get user information
   * @return user information
   * @throws Exception if something goes wrong while fetching user info
   */
  public UserInfo getUserInfo(String username) throws Exception;

  /**
   * Closes resources opened during initialization.  This method will be called
   * on webapp unloading if the org.gusdb.oauth2.server.ApplicationListener is
   * added as a listener in web.xml.
   */
  public void close();

}
