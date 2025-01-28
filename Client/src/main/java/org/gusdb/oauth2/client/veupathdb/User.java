package org.gusdb.oauth2.client.veupathdb;

import org.gusdb.oauth2.client.ValidatedToken;

/**
 * Represents a VEuPathDB authenticated user.  Contains all user information and
 * a validated authentication token which can be used to perform actions on the
 * user's behalf.
 *
 * @author rdoherty
 */
public interface User extends UserInfo {

  ValidatedToken getAuthenticationToken();

}
