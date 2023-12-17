package org.gusdb.oauth2;

import java.util.Map;

import javax.json.JsonValue;

/**
 * Provides methods for standard user information plus supplemental fields
 * 
 * @author rdoherty
 */
public interface UserInfo {
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
