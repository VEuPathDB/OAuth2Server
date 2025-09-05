package org.gusdb.oauth2.eupathdb;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.oauth2.eupathdb.accountdb.UserProfile;

public class TestAuthenticator extends AccountDbAuthenticator {

  private static Logger LOG = LogManager.getLogger(TestAuthenticator.class);

  // maps from username -> [ userid, password ]
  private static final Map<String, TwoTuple<Long, String[]>> USERS =
      new MapBuilder<String, TwoTuple<Long, String[]>>()
      .put("caurreco", new TwoTuple<>(1L, new String[]{ "12345", "Christina" }))
      .put("dfaulk", new TwoTuple<>(2L, new String[]{ "12345", "Dave" }))
      .put("xingao", new TwoTuple<>(3L, new String[]{ "12345", "Jerric" }))
      .put("rdoherty", new TwoTuple<>(4L, new String[]{ "12345", "Ryan" }))
      .put("sfischer", new TwoTuple<>(5L, new String[]{ "12345", "Steve"}))
      .toMap();

  @Override
  protected void initialize(ConnectionPoolConfig dbConfig, String userSchema, List<String> adminUserIds) {
    LOG.info("Authenticator initialized with userSchema '" + userSchema + "' and dbConfig:\n" + dbConfig);
  }

  @Override
  public Optional<String> isCredentialsValid(String username, String password) throws Exception {
    return Optional.ofNullable(getUserData(username, password, true)).map(profile -> profile.getUserId().toString());
  }

  @Override
  protected UserProfile getUserProfile(String username) {
    return getUserData(username, null, false);
  }

  private static UserProfile getUserData(String username, String password, boolean checkPassword) {
    LOG.info("Request to get user id with [" + username + ", " + password + ", " + checkPassword + "]");
    if (username == null) return null;
    TwoTuple<Long, String[]> profile = USERS.get(username);
    if (profile == null) return null;
    if (checkPassword && !profile.getSecond()[0].equals(password)) return null;
    UserProfile data = new UserProfile();
    data.setUserId(profile.getFirst());
    data.setProperties(new MapBuilder<String,String>()
        .put("firstName", profile.getSecond()[1])
        .put("organization", "EuPathDB")
        .toMap());
    return data;
  }

  @Override
  public void overwritePassword(String username, String newPassword) {
    TwoTuple<Long, String[]> user = USERS.get(username);
    user.getSecond()[0] = newPassword;
  }

  @Override
  public List<String> getAdminUserIds() {
    return Collections.emptyList();
  }

  @Override
  public void close() {
    // don't need to do anything
  }
}
