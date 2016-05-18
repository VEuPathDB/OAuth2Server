package org.gusdb.oauth2.wdk;

import java.util.Map;

import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestAuthenticator extends UserDbAuthenticator {

  private static Logger LOG = LoggerFactory.getLogger(TestAuthenticator.class);

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
  protected void initialize(ConnectionPoolConfig dbConfig, String userSchema) {
    LOG.info("Authenticator initialized with userSchema '" + userSchema + "' and dbConfig:\n" + dbConfig);
  }

  @Override
  protected UserDbData getUserData(String username, String password, boolean checkPassword) {
    LOG.info("Request to get user id with [" + username + ", " + password + ", " + checkPassword + "]");
    if (username == null) return null;
    TwoTuple<Long, String[]> profile = USERS.get(username);
    if (profile == null) return null;
    if (checkPassword && !profile.getSecond()[0].equals(password)) return null;
    UserDbData data = new UserDbData();
    data.userId = profile.getFirst();
    data.firstName = profile.getSecond()[1];
    data.organization = "EuPathDB";
    return data;
  }

  @Override
  public void overwritePassword(String username, String newPassword) {
    TwoTuple<Long, String[]> user = USERS.get(username);
    user.getSecond()[0] = newPassword;
  }

  @Override
  public void close() {
    // don't need to do anything
  }
}
