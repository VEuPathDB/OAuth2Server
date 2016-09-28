package org.gusdb.oauth2.wdk;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
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

  private static final Logger LOG = Logger.getLogger(UserDbAuthenticator.class);

  private static enum JsonKey {
    login,
    password,
    connectionUrl,
    platform,
    poolSize,
    userSchema
  }

  protected static class UserDbData {
    public Long userId;
    public String firstName;
    public String middleName;
    public String lastName;
    public String organization;

    public String getName() {
      String name = null;
      if (firstName != null && !firstName.isEmpty()) name = firstName;
      if (middleName != null && !middleName.isEmpty()) {
        name = (name == null ? middleName : name + " " + middleName);
      }
      if (lastName != null && !lastName.isEmpty()) {
        name = (name == null ? lastName : name + " " + lastName);
      }
      return name;
    }

    @Override
    public String toString() {
      return Json.createObjectBuilder()
          .add("userId", userId)
          .add("name", getName())
          .add("organization", organization)
          .build().toString();
    }
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
    return getUserData(username, password, true) != null;
  }

  @Override
  public UserInfo getUserInfo(final String username) throws Exception {
    final UserDbData userData = getUserData(username, "", false);
    if (userData == null) {
      throw new IllegalStateException("User could not be found even though already authenticated.");
    }
    return new UserInfo() {
      @Override
      public String getUserId() {
        return String.valueOf(userData.userId);
      }
      @Override
      public String getEmail() {
        // username is email address in User DB
        return username;
      }
      @Override
      public boolean isEmailVerified() {
        return true;
      }
      @Override
      public Map<String, JsonValue> getSupplementalFields() {
        JsonObject json = Json.createObjectBuilder()
            .add("name", userData.getName())
            .add("organization", userData.organization)
            .build();
        // convert JSON object to map of String -> JsonValue
        return new MapBuilder<String, JsonValue>()
            .put("name", json.getJsonString("name"))
            .put("organization", json.getJsonString("organization"))
            .toMap();
      }
    };
  }

  protected UserDbData getUserData(String username, String password, boolean checkPassword) {
    String sql = "select user_id, first_name, middle_name, last_name, organization from " + _userSchema + "users where email = ?";
    if (checkPassword) sql += " and passwd = ?";
    Object[] params = (checkPassword ?
        new Object[]{ username, encryptPassword(password) } :
        new Object[]{ username });
    final TwoTuple<Boolean, UserDbData> result = new TwoTuple<>(false, null);
    new SQLRunner(_userDb.getDataSource(), sql)
      .executeQuery(params, new ResultSetHandler() {
        @Override public void handleResult(ResultSet rs) throws SQLException {
          if (rs.next()) {
            UserDbData userData = new UserDbData();
            userData.userId = rs.getLong(1);
            userData.firstName = rs.getString(2);
            userData.middleName = rs.getString(3);
            userData.lastName = rs.getString(4);
            userData.organization = rs.getString(5);
            result.set(true, userData);
          }
        }
      });
    LOG.debug("Checking submitted credentials of " + username +
        ".  Success? " + result.getFirst() + ", user: " + result.getSecond());
    return (result.getFirst() ? result.getSecond() : null);
  }

  // copied from WDK's UserFactory class
  private static String encryptPassword(String str) {
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

  @Override
  public void overwritePassword(String username, String newPassword) throws Exception {
    newPassword = encryptPassword(newPassword);
    String sql = "update " + _userSchema + "users set passwd = ? where email = ?";
    new SQLRunner(_userDb.getDataSource(), sql).executeUpdate(new Object[]{ newPassword, username });
  }
}
