package org.gusdb.oauth2.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages state on the OAuth server.  This includes OAuth server sessions,
 * their owners, and associated authentication codes and tokens, as well as
 * pending login form IDs and their associated metadata.  This exists in the
 * following states:
 * 
 * 1. Client is not authenticated, no forms yet requested
 *     a. Session object exists, but has no attributes
 *     b. Pending form map is empty
 *     c. No auth code or token associated with user
 * 2. Client has requested a form
 *     a. Session object exists, contains pending form map with 1 entry
 *     b. No auth code or token associated with user
 * 3. Client successfully submits form
 *     a. Form map is cleared of that entry
 *     b. If user didn't already succeed elsewhere:
 *         i. Username associated with session
 *         ii. Auth code generated 
 * 
 * @author ryan
 */
public class StateCache {

  /*
   * HttpSession contains the username (string used in username field of
   * submitted form) of a user if the user has successfully authenticated
   * in the given session.  Any forms sent out before then are either
   * anonymous (accessed directly by client), or identified by an ID as
   * part of an OAuth authentication flow.  The ID is keyed to a set of
   * parameters sent as part of an authentication request and is used to
   * redirect the user to the proper location (with an auth code) once
   * he has successfully authenticated.  Form IDs are retained as part of
   * the session until the form is submitted, or until the session expires
   * or the user logs out.
   */

  private static final List<String> AUTH_CODES = new ArrayList<String>();
  private static final List<String> TOKENS = new ArrayList<String>();

  public static synchronized void addAuthCode(String authorizationCode) {
    AUTH_CODES.add(authorizationCode);
  }

  public static void addToken(String accessToken) {
    TOKENS.add(accessToken);
  }

  public static boolean isValidToken(String accessToken) {
    return TOKENS.contains(accessToken);
  }

  public static boolean checkAuthCode(String authorizationCode) {
    return AUTH_CODES.contains(authorizationCode);
  }

}
