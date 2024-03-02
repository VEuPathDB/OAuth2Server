package org.gusdb.oauth2.client.veupathdb;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.client.OAuthClient;
import org.gusdb.oauth2.client.ValidatedToken;
import org.gusdb.oauth2.shared.IdTokenFields;
import org.json.JSONObject;

public class BearerTokenUser extends BasicUser {

  private static final Logger LOG = LogManager.getLogger(BearerTokenUser.class);

  private final OAuthClient _client;
  private final String _oauthUrl;
  private final ValidatedToken _token;
  private boolean _userInfoFetched = false;

  public BearerTokenUser(OAuthClient client, String oauthUrl, ValidatedToken token) {
    // parent constructor sets immutable fields provided on the token
    super(
        Long.valueOf(token.getUserId()),
        token.isGuest(),
        token.getTokenContents().get(IdTokenFields.signature.name(), String.class),
        token.getTokenContents().get(IdTokenFields.preferred_username.name(), String.class));
    _client = client;
    _oauthUrl = oauthUrl;
    _token = token;
  }

  @Override
  protected void fetchUserInfo() {
    // return if already fetched
    if (_userInfoFetched) return;

    LOG.info("User data fetch requested for user " + getUserId() + "; querying OAuth server.");
    // fetch user info from OAuth server where it is stored (but only on demand, and only once for this object's lifetime)
    JSONObject userInfo = _client.getUserData(_oauthUrl, _token);

    // set values found only on user info object
    setPropertyValues(userInfo);

    _userInfoFetched = true;

    LOG.info("User data successfully fetched for " + getDisplayName() + " / " + getOrganization());
  }

}

