package org.gusdb.oauth2;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.gusdb.oauth2.service.UserPropertiesRequest;

/**
 * Provides access to a custom user store to authenticate users and provide
 * basic user information to approved clients.  Users of the OAuth2 server
 * must provide an implementation of this class specific to their user storage
 * mechanism.
 * 
 * @author rdoherty
 */
public interface Authenticator {

  public static class RequestingUser extends AbstractMap.SimpleImmutableEntry<String, Boolean> {
    public RequestingUser(String userId, boolean isGuest) { super(userId, isGuest); }
    public String getUserId() { return getKey(); }
    public boolean isGuest() { return getValue(); }
  }

  /**
   * Provides methods for standard user information plus supplemental fields
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
     * Whether the user is a guest user vs a registered user
     *
     * @return true if user is a guest, else false
     */
    public boolean isGuest();

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
     * Returns a human-readable username for the user.  The value returned will
     * be used as the "preferred_username" property of the OpenID Connect ID
     * token.  If null or an empty String is returned, the "preferred_username"
     * property will be omitted.  Per the spec, implementations are not required
     * to make this value unique or stable.  However, they may depending on
     * their need.
     * 
     * @return users's preferred username
     */
    public String getPreferredUsername();


    /**
     * Returns a "user signature", meant to be a stable identifier that is not
     * guessable (like numeric user ID) nor human readable (like stable ID aka
     * preferredUsername).  This has only a few legacy purposes (maybe just user
     * comment files) and should eventually be removed, but it remains for now.
     *
     * @return user signature
     */
    public String getSignature();

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
   * Replies with a non-empty optional containing the user ID of a user
   * if the passed credentials identify a valid user, else an empty optional.
   * The passed strings are not checked for SQL-injection or other hacks.
   * 
   * @param username entered username
   * @param password entered password
   * @return user ID if passed creds identify a user, else empty
   * @throws Exception if something goes wrong during validation
   */
  public Optional<String> isCredentialsValid(String username, String password) throws Exception;

  /**
   * Returns implementation-specific user information used to populate an ID
   * token.  Only the user ID field of the returned object is required.  Email
   * and EmailVerified are optional. Authenticator implementations can also add
   * supplemental fields in the supplementalFields property of the returned object.
   * 
   * This information is used to populate an OpenID Connect ID token, which is
   * returned for /token requests if the "includeUserInfoWithToken" config value
   * is true, and is returned for /user requests accompanied by a valid access
   * token.
   * 
   * @param username username for which to get user information
   * @return user information to populate an ID token
   * @throws Exception if something goes wrong while fetching user info
   */
  public UserInfo getTokenInfo(String username) throws Exception;

  /**
   * Returns implementation-specific user information used to populate a user
   * profile object.  Only the user ID field of the returned object is required.  Email
   * and EmailVerified are optional. Authenticator implementations can also add
   * supplemental fields in the supplementalFields property of the returned object.
   * 
   * This information is returned by the /user endpoint if a valid token is presented
   * as part of the request.  To keep OIDC/bearer tokens small, this method can/should
   * contain a more comprehensive profile and/or any larger fields than the UserInfo
   * returned by the getTokenInfo() method.
   * 
   * @param userId user ID for which to get user information
   * @return user information returned by the /user endpoint
   * @throws Exception if something goes wrong while fetching user info
   */
  public UserInfo getProfileInfo(String userId) throws Exception;

  /**
   * Returns the user profile (returned by the /user endpoint) for a guest user.  The
   * default method stubs empty values for all methods except getUserId() which returns
   * the passed value, and getPreferredUsername() which returns "guest-" + getUserId()
   *
   * @param userId user ID of the guest user
   * @return user profile for a guest user with the passed ID
   */
  public UserInfo getGuestProfileInfo(String userId);

  /**
   * Overwrites user's password in the system.  The passed strings are not
   * checked for SQL-injection or other hacks prior to calling this method.
   * 
   * @param username username of user whose password should be overwritten
   * @param newPassword new password for the user
   */
  public void overwritePassword(String username, String newPassword) throws Exception;

  /**
   * Closes resources opened during initialization.  This method will be called
   * on webapp unloading if the org.gusdb.oauth2.server.ApplicationListener is
   * added as a listener in web.xml.
   */
  public void close();

  /**
   * Executes an arbitrary query against the account data store for an approved
   * and authenticated client
   * 
   * @param querySpec configuration of the query
   * @return query response
   * @throws UnsupportedOperationException if queries are not supported by this
   * authenticator implementation
   * @throws JsonParsingException if querySpec is invalid
   */
  public JsonObject executeQuery(JsonObject querySpec)
      throws UnsupportedOperationException, JsonParsingException;

  /**
   * Allows authenticator to log successful logins in an application specific way
   *
   * @param username username that successfully logged in
   * @param clientId ID of OAuth client performing login
   * @param redirectUri URL auth response will be sent to
   * @param requestingIpAddress IP address making the request
   */
  public void logSuccessfulLogin(String username, String clientId, String redirectUri, String requestingIpAddress);

  /**
   * Returns true if getNextGuestId() is implemented to produce guest IDs.  If this method
   * returns false, the guest token endpoint of this OIDC server will throw an error
   */
  public default boolean supportsGuests() {
    return false;
  }

  /**
   * @return a new guest ID
   */
  public default String getNextGuestId() {
    throw new UnsupportedOperationException("This authenticator does not support guests.");
  }

  /**
   * Creates a new account from the given request and returns a UserInfo object representing it
   *
   * @param userProps properties from which to create new account
   * @param initialPassword initial password to associate with the account
   * @return object representing the new user
   * @throws IllegalArgumentException if input user props are invalid
   * @throws RuntimeException if unable to complete the operation
   */
  public UserInfo createUser(UserPropertiesRequest userProps, String initialPassword) throws IllegalArgumentException;

  /**
   * Modifies the user account for passed userId using the passed user props
   *
   * @param userId ID of user to modify
   * @param userProps properties to assign to the specified user
   * @return object representing the modified user
   * @throws IllegalArgumentException if input user props are invalid
   * @throws RuntimeException if unable to complete the operation
   */
  public UserInfo modifyUser(String userId, UserPropertiesRequest userProps) throws IllegalArgumentException;

}
