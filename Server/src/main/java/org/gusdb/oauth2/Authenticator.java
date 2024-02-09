package org.gusdb.oauth2;

import java.util.AbstractMap;
import java.util.Optional;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.gusdb.oauth2.service.UserPropertiesRequest;

/**
 * Provides access to a custom user store to authenticate users and provide
 * basic user information to approved clients.  Users of the OAuth2 server
 * must provide an implementation of this class specific to their user storage
 * mechanism.
 * 
 * @author rdoherty
 */
public interface Authenticator extends AutoCloseable {

  public enum DataScope {
    /**
     * Indicates implementation-specific user information used to populate OIDC ID and
     * bearer tokens.  Only the user ID field of the returned object is required.  Email
     * and EmailVerified are optional. Authenticator implementations can also add
     * supplemental fields in the supplementalFields property of the returned object.
     *
     * This information is used to populate both types of OpenIDConnect tokens.
     * - ID tokens:     returned for /token requests
     * - Bearer tokens: returned for /bearer-token requests
     *
     * To keep OIDC ID/bearer tokens small, this scope can/should produce a less
     * comprehensive collection of fields than the PROFILE scope and omit any large fields.
     */
    TOKEN,

    /**
     * Indicates implementation-specific user information used to populate a user
     * profile object.  Only the user ID field of the returned object is required.  Email
     * and EmailVerified are optional. Authenticator implementations can also add
     * supplemental fields in the supplementalFields property of the returned object.
     *
     * This information is returned by user requests (registration, info, edit requests)
     * if a valid token is presented as part of the request.
     *
     * To keep OIDC ID/bearer tokens small, this scope can/should produce a more
     * comprehensive collection of fields than the TOKEN scope and include any large fields.
     */
    PROFILE;
  }

  public static class RequestingUser extends AbstractMap.SimpleImmutableEntry<String, Boolean> {
    public RequestingUser(String userId, boolean isGuest) { super(userId, isGuest); }
    public String getUserId() { return getKey(); }
    public boolean isGuest() { return getValue(); }
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
   * @param loginName entered login value
   * @param password entered password
   * @return user ID if passed creds identify a user, else empty
   * @throws Exception if something goes wrong during validation
   */
  public Optional<String> isCredentialsValid(String loginName, String password) throws Exception;

  /**
   * Look up user information by login value.
   *
   * @param loginName login value for which user information should be retrieved
   * @return user information for the given scope, or empty optional if not found
   * @throws Exception if something goes wrong while fetching user info
   */
  public Optional<UserInfo> getUserInfoByLoginName(String loginName, DataScope scope) throws Exception;

  /**
   * Look up user information by user ID.
   *
   * @param userId user ID for which user information should be retrieved
   * @return user information for the given scope, or empty optional if not found
   * @throws Exception if something goes wrong while fetching user info
   */
  public Optional<UserInfo> getUserInfoByUserId(String userId, DataScope scope) throws Exception;

  /**
   * Looks up the user ID (must be a guest) and returns the user profile (returned by
   * the /user endpoint) for a guest user if found.  If not found, an empty optional
   * is returned.  The default method always returns an empty optional.
   *
   * @param userId user ID of the guest user
   * @return user profile for a guest user with the passed ID, or empty if not found
   */
  public default Optional<UserInfo> getGuestProfileInfo(String userId) {
    return Optional.empty();
  }

  /**
   * Overwrites user's password in the system.  The passed strings are not
   * checked for SQL-injection or other hacks prior to calling this method.
   * 
   * @param userId ID of user whose password should be overwritten
   * @param newPassword new password for the user
   */
  public void overwritePassword(String userId, String newPassword) throws Exception;

  /**
   * Closes resources opened during initialization.  This method will be called
   * on webapp unloading if the org.gusdb.oauth2.server.ApplicationListener is
   * added as a listener in web.xml.
   */
  @Override
  public void close();

  /**
   * Executes an arbitrary query against the account data store for an approved
   * and authenticated client
   * 
   * @param querySpec configuration of the query
   * @return query response
   * @throws UnsupportedOperationException if queries are not supported by this
   * authenticator implementation
   * @throws IllegalArgumentException if querySpec is invalid
   */
  public JsonValue executeQuery(JsonObject querySpec)
      throws UnsupportedOperationException, IllegalArgumentException;

  /**
   * Allows authenticator to log successful logins in an application specific way
   *
   * @param loginName login value used to successfully log in
   * @param userId ID of user the performed successful login
   * @param clientId ID of OAuth client performing login
   * @param redirectUri URL auth response will be sent to
   * @param requestingIpAddress IP address making the request
   */
  public void logSuccessfulLogin(String loginName, String userId, String clientId, String redirectUri, String requestingIpAddress);

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

  /**
   * Reset the password for the passed login to the passed password value
   *
   * @param loginName login for which to change password
   * @param newPassword new password
   */
  public void resetPassword(String loginName, String newPassword);

  /**
   * Generate a new password.  Used for initial user password and password reset.
   *
   * @return new, random password
   */
  public String generateNewPassword();

  /**
   * Update last login timestamp for the user with the passed user ID.  Implementation of
   * this method is optional (simply no-op if unsupported).  It is called each time a
   * new token (ID token or bearer token) is produced.
   *
   * @param userId user for whom a new token was generated
   */
  public void updateLastLoginTimestamp(String userId);

}
