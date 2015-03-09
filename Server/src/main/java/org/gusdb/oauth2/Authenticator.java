package org.gusdb.oauth2;

import javax.json.JsonObject;

/**
 * Provides access to a custom user store to authenticate users and provide
 * basic user information to approved clients.  Users of the OAuth2 server
 * must provide an implementation of this class specific to their user storage
 * mechanism.
 * 
 * @author ryan
 */
public interface Authenticator {

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
   * Returns implementation-specific user information.  This is the JSON
   * returned for /user requests when accompanied by a valid access token.  The
   * implementation can provide as little or as much information about the
   * user as it likes (i.e. user id, name, display name, email, etc.)
   * 
   * @param username username for which to get user information
   * @return user information in JSON format
   * @throws Exception if something goes wrong while fetching user info
   */
  public JsonObject getUserInfo(String username) throws Exception;

  /**
   * Closes resources opened during initialization.  This method will be called
   * on webapp unloading if the org.gusdb.oauth2.server.ApplicationListener is
   * added as a listener in web.xml.
   */
  public void close();

}
