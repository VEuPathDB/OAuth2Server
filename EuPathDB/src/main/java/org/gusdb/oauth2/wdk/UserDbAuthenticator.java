package org.gusdb.oauth2.wdk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ResultSetHandler;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.InitializationException;

public class UserDbAuthenticator implements Authenticator {

  private static enum JsonKey {
    login,
    password,
    connectionUrl,
    platform,
    poolSize,
    userSchema
  }

  private DatabaseInstance _userDb;
  private String _userSchema;

  @Override
  public void initialize(JsonObject configJson) throws InitializationException {
    ConnectionPoolConfig dbConfig = SimpleDbConfig.create(
        SupportedPlatform.toPlatform(configJson.getString(JsonKey.platform.name())),
        configJson.getString(JsonKey.connectionUrl.name()),
        configJson.getString(JsonKey.login.name()),
        configJson.getString(JsonKey.password.name()),
        (short)configJson.getInt(JsonKey.poolSize.name()));
    String userSchema = configJson.getString(JsonKey.userSchema.name());
    if (!userSchema.isEmpty() && !userSchema.endsWith(".")) userSchema += ".";
    initialize(dbConfig, userSchema);
  }

  protected void initialize(ConnectionPoolConfig dbConfig, String userSchema) {
    _userDb = new DatabaseInstance(dbConfig, true);
    _userSchema = userSchema;
  }

  // WDK uses email and password
  @Override
  public boolean isCredentialsValid(String username, String password) throws Exception {
    return getUserId(username, password, true) != null;
  }

  @Override
  public UserInfo getUserInfo(String username) throws Exception {
    final Long id = getUserId(username, "", false);
    if (id == null) {
      throw new IllegalStateException("User could not be found even though already authenticated.");
    }
    return new UserInfo() {
      @Override
      public String getUserId() {
        return String.valueOf(id);
      }
      @Override
      public String getEmail() {
        // shouldn't need email for Api sites; hide from Globus
        //return username;
        return null;
      }
      @Override
      public boolean isEmailVerified() {
        return true;
      }
      @Override
      public Map<String, JsonValue> getSupplementalFields() {
        return Collections.EMPTY_MAP;
      }
    };
  }

  protected Long getUserId(String username, String password, boolean checkPassword) {
    String sql = "select user_id from " + _userSchema + "users where email = ?";
    if (checkPassword) sql += " and passwd = ?";
    Object[] params = (checkPassword ?
        new Object[]{ username, encrypt(password) } :
        new Object[]{ username });
    final TwoTuple<Boolean, Long> result = new TwoTuple<>(false, null);
    new SQLRunner(_userDb.getDataSource(), sql )
      .executeQuery(params, new ResultSetHandler() {
        @Override public void handleResult(ResultSet rs) throws SQLException {
          if (rs.next()) result.set(true, rs.getLong(1));
        }
      });
    return (result.getFirst() ? result.getSecond() : null);
  }

  // copied from WDK's UserFactory class
  private static String encrypt(String str) {
    String algorithm = "MD5";
    try {
      // convert each byte into hex format
      MessageDigest digest = MessageDigest.getInstance(algorithm);
      StringBuilder buffer = new StringBuilder();
      for (byte code : digest.digest(str.getBytes())) {
        buffer.append(Integer.toHexString(code & 0xFF));
      }
      return buffer.toString();
    }
    catch (NoSuchAlgorithmException e) {
      // this should never happen
      throw new RuntimeException("Unable to initialize MessageDigest with algorithm " + algorithm, e);
    }
  }

  @Override
  public void close() {
    if (_userDb != null) {
      try {
        _userDb.close();
      }
      catch (Exception e) {
        throw new RuntimeException("Unable to properly close configured database instance", e);
      }
    }
  }
}
