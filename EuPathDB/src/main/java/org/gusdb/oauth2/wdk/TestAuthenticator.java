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
  private static final Map<String, TwoTuple<Long, String>> USERS =
      new MapBuilder<String, TwoTuple<Long, String>>()
      .put("caurreco", new TwoTuple<>(1L, "12345"))
      .put("dfaulk", new TwoTuple<>(2L, "12345"))
      .put("xingao", new TwoTuple<>(3L, "12345"))
      .put("rdoherty", new TwoTuple<>(4L, "12345"))
      .put("sfischer", new TwoTuple<>(5L, "12345"))
      .toMap();

  @Override
  protected void initialize(ConnectionPoolConfig dbConfig, String userSchema) {
    LOG.info("Authenticator initialized with userSchema '" + userSchema + "' and dbConfig:\n" + dbConfig);
  }

  @Override
  protected Long getUserId(String username, String password, boolean checkPassword) {
    LOG.info("Request to get user id with [" + username + ", " + password + ", " + checkPassword + "]");
    if (username == null) return null;
    TwoTuple<Long, String> profile = USERS.get(username);
    if (profile == null) return null;
    if (checkPassword && !profile.getSecond().equals(password)) return null;
    return profile.getFirst();
  }

  @Override
  public void close() {
    // don't need to do anything
  }
}
