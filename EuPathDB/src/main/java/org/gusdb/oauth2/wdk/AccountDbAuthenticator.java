package org.gusdb.oauth2.wdk;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.accountdb.AccountManager;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.gusdb.fgputil.accountdb.UserPropertyName;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.InitializationException;

public class AccountDbAuthenticator implements Authenticator {

  private static final Logger LOG = Logger.getLogger(AccountDbAuthenticator.class);

  private static enum JsonKey {
    login,
    password,
    connectionUrl,
    platform,
    poolSize,
    schema
  }

  private static final List<UserPropertyName> USER_PROPERTIES = Arrays.asList(new UserPropertyName[] {
      new UserPropertyName("firstName", "first_name", true),
      new UserPropertyName("middleName", "middle_name", false),
      new UserPropertyName("lastName", "last_name", true),
      new UserPropertyName("organization", "organization", true)
  });

  private DatabaseInstance _accountDb;
  private String _schema;

  @Override
  public void initialize(JsonObject configJson) throws InitializationException {
    ConnectionPoolConfig dbConfig = SimpleDbConfig.create(
        SupportedPlatform.toPlatform(configJson.getString(JsonKey.platform.name())),
        configJson.getString(JsonKey.connectionUrl.name()),
        configJson.getString(JsonKey.login.name()),
        configJson.getString(JsonKey.password.name()),
        (short)configJson.getInt(JsonKey.poolSize.name()));
    String schema = configJson.getString(JsonKey.schema.name());
    if (!schema.isEmpty() && !schema.endsWith(".")) schema += ".";
    initialize(dbConfig, schema);
  }

  protected void initialize(ConnectionPoolConfig dbConfig, String schema) {
    LOG.info("Initializing database using: " + dbConfig);
    _accountDb = new DatabaseInstance(dbConfig, true);
    _schema = schema;
  }

  // WDK uses email and password
  @Override
  public boolean isCredentialsValid(String username, String password) throws Exception {
    return new AccountManager(_accountDb, _schema, USER_PROPERTIES).getUserProfile(username, password) != null;
  }

  @Override
  public UserInfo getUserInfo(final String username) throws Exception {
    final UserProfile profile = getUserProfile(username);
    if (profile == null) {
      throw new IllegalStateException("User could not be found even though already authenticated.");
    }
    return new UserInfo() {
      @Override
      public String getUserId() {
        return String.valueOf(profile.getUserId());
      }
      @Override
      public String getEmail() {
        // username is email address in Account DB
        return username;
      }
      @Override
      public boolean isEmailVerified() {
        return true;
      }
      @Override
      public String getPreferredUsername() {
        // stable value that EuPathDB can use to identify this user
        return profile.getStableId();
      }
      @Override
      public Map<String, JsonValue> getSupplementalFields() {
        JsonObject json = Json.createObjectBuilder()
            .add("name", getDisplayName(profile.getProperties()))
            .add("organization", profile.getProperties().get("organization"))
            .build();
        // convert JSON object to map of String -> JsonValue
        return new MapBuilder<String, JsonValue>()
            .put("name", json.getJsonString("name"))
            .put("organization", json.getJsonString("organization"))
            .toMap();
      }
    };
  }

  // protected so TestAuthenticator can override
  protected UserProfile getUserProfile(String username) {
    return new AccountManager(_accountDb, _schema, USER_PROPERTIES).getUserProfile(username);
  }

  private static String getDisplayName(Map<String,String> userProperties) {
    String firstName = userProperties.get("firstName");
    String middleName = userProperties.get("middleName");
    String lastName = userProperties.get("lastName");
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
  public void close() {
    if (_accountDb != null) {
      try {
        _accountDb.close();
      }
      catch (Exception e) {
        throw new RuntimeException("Unable to properly close configured database instance", e);
      }
    }
  }

  @Override
  public void overwritePassword(String username, String newPassword) throws Exception {
    UserProfile profile = getUserProfile(username);
    new AccountManager(_accountDb, _schema, USER_PROPERTIES).updatePassword(profile.getUserId(), newPassword);
  }

  @Override
  public JsonObject executeQuery(JsonObject querySpec)
      throws UnsupportedOperationException, JsonParsingException {
    long requestedUserId = Long.valueOf(querySpec.getInt("userId"));
    UserProfile user = new AccountManager(_accountDb, _schema, USER_PROPERTIES)
        .getUserProfile(requestedUserId);
    if (user == null) {
      return Json.createObjectBuilder()
          .add("found", false)
          .build();
    }
    else {
      return Json.createObjectBuilder()
          .add("found", true)
          .add("email", user.getEmail())
          .add("name", getDisplayName(user.getProperties()))
          .add("organization", user.getProperties().get("organization"))
          .build();
    }
  }
}
