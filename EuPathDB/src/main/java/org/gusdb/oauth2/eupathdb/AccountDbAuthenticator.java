package org.gusdb.oauth2.eupathdb;

import static org.gusdb.fgputil.FormatUtil.getInnerClassLog4jName;

import java.net.URI;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.stream.JsonParsingException;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.accountdb.AccountManager;
import org.gusdb.fgputil.accountdb.UserProfile;
import org.gusdb.fgputil.accountdb.UserPropertyName;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunnerException;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.UserInfo;
import org.gusdb.oauth2.client.veupathdb.User;
import org.gusdb.oauth2.service.OAuthRequestHandler;
import org.gusdb.oauth2.service.UserPropertiesRequest;
import org.gusdb.oauth2.shared.IdTokenFields;

public class AccountDbAuthenticator implements Authenticator {

  private static final Logger LOG = Logger.getLogger(AccountDbAuthenticator.class);

  // create specially scoped Logger to write the login recording log
  private static class LoginLogger {}
  private static final Logger LOGIN_LOG = Logger.getLogger(getInnerClassLog4jName(LoginLogger.class));

  private static enum JsonKey {
    login,
    password,
    connectionUrl,
    platform,
    poolSize,
    schema
  }

  // convert from new API to old
  private static final List<UserPropertyName> USER_PROPERTY_DEFS =
      User.USER_PROPERTIES.values().stream()
      .map(p -> new UserPropertyName(p.getName(), p.getDbKey(), p.isRequired()))
      .collect(Collectors.toList());

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
    initialize(dbConfig, schema);
  }

  protected void initialize(ConnectionPoolConfig dbConfig, String schema) {
    LOG.info("Initializing database using: " + dbConfig);
    _accountDb = new DatabaseInstance(dbConfig, true);
    if (!schema.isEmpty() && !schema.endsWith(".")) schema += ".";
    _schema = schema;
  }

  // WDK uses email and password
  @Override
  public Optional<String> isCredentialsValid(String username, String password) throws Exception {
    return Optional.ofNullable(
        new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).getUserProfile(username, password))
      .map(profile -> profile.getUserId().toString());
  }

  @Override
  public Optional<UserInfo> getUserInfoByLoginName(String loginName, DataScope scope) throws Exception {
    return getUserInfo(new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).getUserProfileByUsernameOrEmail(loginName), scope);
  }

  @Override
  public Optional<UserInfo> getUserInfoByUserId(String userId, DataScope scope) throws Exception {
    return getUserInfo(new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).getUserProfile(Long.valueOf(userId)), scope);
  }

  @Override
  public void resetPassword(String userId, String newPassword) {
    new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).updatePassword(Long.valueOf(userId), newPassword);
  }

  @Override
  public String generateNewPassword() {
    // generate a random password of 12 characters, each in the range [0-9A-Za-z]
    StringBuilder buffer = new StringBuilder();
    Random rand = new Random();
    for (int i = 0; i < 12; i++) {
      int value = rand.nextInt(62);
      if (value < 10) { // number
        buffer.append(value);
      }
      else if (value < 36) { // upper case letters
        buffer.append((char) ('A' + value - 10));
      }
      else { // lower case letters
        buffer.append((char) ('a' + value - 36));
      }
    }
    return buffer.toString();
  }

  private Optional<UserInfo> getUserInfo(UserProfile profile, DataScope scope) {
    return Optional.ofNullable(profile).map(p -> createUserInfoObject(p, true, scope));
  }

  private UserInfo createUserInfoObject(UserProfile profile, boolean isEmailVerified, DataScope scope) {
    return new UserInfo() {
      @Override
      public String getUserId() {
        return String.valueOf(profile.getUserId());
      }
      @Override
      public boolean isGuest() {
        return profile.isGuest();
      }
      @Override
      public String getEmail() {
        // username is email address in Account DB
        return profile.getEmail();
      }
      @Override
      public boolean isEmailVerified() {
        return isEmailVerified;
      }
      @Override
      public String getPreferredUsername() {
        // stable value that EuPathDB can use to identify this user
        return profile.getStableId();
      }
      @Override
      public String getSignature() {
        return profile.getSignature();
      }
      @Override
      public Map<String, JsonValue> getSupplementalFields() {
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("name", getDisplayName(profile.getProperties()))
            .add("organization", profile.getProperties().get("organization"));
        if (scope == DataScope.PROFILE) builder
            .add("firstName", profile.getProperties().get("firstName"))
            .add("middleName", profile.getProperties().get("middleName"))
            .add("lastName", profile.getProperties().get("lastName"))
            .add("username", profile.getProperties().get("username"))
            .add("interests", profile.getProperties().get("interests"));
        JsonObject json = builder.build();
        // convert JSON object to map of String -> JsonValue
        Map<String,JsonValue> map = new HashMap<>();
        for (String key : json.keySet()) {
          // we know they are all strings 
          map.put(key, json.getJsonString(key));
        }
        return map;
      }
    };
  }

  // protected so TestAuthenticator can override
  protected UserProfile getUserProfile(String userId) {
    return new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).getUserProfile(Long.valueOf(userId));
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
  public boolean supportsGuests() {
    return true;
  }

  /**
   * @return a new guest ID
   */
  @Override
  public String getNextGuestId() {
    try {
      String id = new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).createGuestAccount("guest_").getUserId().toString();
      // FIXME: since this code directly accesses the DB, it should live in AccountManager;
      //   however that complicates FgpUtil releases prior to move to bearer tokens, so adding it here.
      String sql = "insert into useraccounts.guest_ids (user_id, creation_time) values (?, TO_DATE(SYSDATE))";
      int inserted = new SQLRunner(_accountDb.getDataSource(), sql, "insert-guest-id")
          .executeUpdate(new Object[]{ Long.valueOf(id) }, new Integer[]{ Types.BIGINT });
      if (inserted != 1) throw new IllegalStateException("Tried to insert duplicate guest ID " + id + ". Check ID sequence to make sure it is big enough.");
      return id;
    }
    catch (SQLException e) {
      throw new RuntimeException("Unable to generate next guest ID", e);
    }
    catch (SQLRunnerException e) {
      throw new RuntimeException("Could not insert row to guest_ids", e.getCause());
    }
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
    new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS).updatePassword(profile.getUserId(), newPassword);
  }

  @Override
  public JsonValue executeQuery(JsonObject querySpec)
      throws UnsupportedOperationException, IllegalArgumentException {
    try {
      if ((!querySpec.containsKey("userId") && !querySpec.containsKey("userIds")) ||
          ( querySpec.containsKey("userId") &&  querySpec.containsKey("userIds"))) {
        throw new IllegalArgumentException("Query must contain exactly one of ['userId', 'userIds']");
      }
      AccountManager acctDb = new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS);
      if (querySpec.containsKey("userId")) {
        long requestedUserId = Long.valueOf(querySpec.getInt("userId"));
        return getUserJson(acctDb, requestedUserId);
      }
      else {
        JsonArray inputArray = querySpec.getJsonArray("userIds");
        JsonArrayBuilder outputArray = Json.createArrayBuilder();
        for (int i = 0; i < inputArray.size(); i++) {
          JsonObject userObj = getUserJson(acctDb, Long.valueOf(inputArray.getInt(i)));
          outputArray.add(userObj);
        }
        return outputArray.build();
      }
    }
    catch (JsonParsingException e) {
      throw new IllegalArgumentException("Illegal query; " + e.getMessage());
    }
  }

  private JsonObject getUserJson(AccountManager acctDb, long requestedUserId) {
    // look for registered user first
    UserProfile userProfile = acctDb.getUserProfile(requestedUserId);
    Optional<UserInfo> userOpt = userProfile == null
      // no registered user found; look for guest
      ? getGuestProfileInfo(String.valueOf(requestedUserId))
      // convert profile to UserInfo
      : getUserInfo(userProfile, DataScope.PROFILE);
    return userOpt
      // found a user with this ID
      .map(user -> OAuthRequestHandler.getUserInfoResponseJson(user, Optional.empty())
        .add("found", true)
        .build())
      // did not find user of any type
      .orElse(Json.createObjectBuilder()
        .add(IdTokenFields.sub.name(), String.valueOf(requestedUserId))
        .add("found", false)
        .build());
  }

  @Override
  public void logSuccessfulLogin(String loginName, String userId, String clientId, String redirectUri, String requestingIpAddress) {
    LOGIN_LOG.info(requestingIpAddress + " " + clientId + " " + getHost(redirectUri) + " " + userId + " " + loginName);
  }

  private static String getHost(String uriStr) {
    try {
      return new URI(uriStr).getHost();
    }
    catch(Exception e) {
      LOG.error("Unable to parse passed URI: " + uriStr);
      return "unknown";
    }
  }

  @Override
  public UserInfo createUser(UserPropertiesRequest userProps, String initialPassword) {
    AccountManager accountMgr = new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS);
    validateUserProps(accountMgr, userProps, Optional.empty());
    UserProfile newUser = Functions.mapException(
        () -> accountMgr.createAccount(userProps.getEmail(), initialPassword, userProps),
        e -> new RuntimeException(e)); // all exceptions at this point are 500s
    return createUserInfoObject(newUser, false, DataScope.PROFILE);
  }

  /**
   * Validates user properties, in some cases massaging them to be valid values.  It makes sure
   * required properties are present and non-empty, standardizes email values, and ensures
   * uniqueness of email and username.
   *
   * @param accountMgr account manager providing access to data store
   * @param userProps properties to be validated
   * @param userId user to which properties will be assigned
   */
  private void validateUserProps(AccountManager accountMgr, UserPropertiesRequest userProps, Optional<Long> userId) {

    // check email for uniqueness and format
    userProps.setEmail(validateAndFormatEmail(userProps.getEmail(), accountMgr, userId));

    // if user supplied a username, make sure it is unique
    if (userProps.containsKey(AccountManager.USERNAME_PROPERTY_KEY)) {
      String username = userProps.get(AccountManager.USERNAME_PROPERTY_KEY);
      // check whether the username exists in the database already under a different user
      ensureUniqueValue("username", accountMgr.getUserProfileByUsername(username), username, userId);
    }

    // make sure required props are present; trimming of unsupported keys is done in AccountManager
    // TODO: move this check into AccountManager
    for (UserPropertyName prop : USER_PROPERTY_DEFS) {
      if (prop.isRequired() && (!userProps.containsKey(prop.getName()) || userProps.get(prop.getName()).isBlank())) {
        throw new IllegalArgumentException("User property '" + prop.getName() + "' cannot be empty.");
      }
    }
  }

  private static String validateAndFormatEmail(String email, AccountManager accountMgr, Optional<Long> userId) {
    // trim and validate passed email address and extract stable name
    if (email == null)
      throw new IllegalArgumentException("The user's email cannot be empty.");
    // format the info
    email = AccountManager.trimAndLowercase(email);
    if (email.isEmpty())
      throw new IllegalArgumentException("The user's email cannot be empty.");
    int atSignIndex = email.indexOf("@");
    if (atSignIndex < 1) // must be present and not the first char
      throw new IllegalArgumentException("The user's email address is invalid.");
    if (atSignIndex > email.length() - 4) // must be at least a@b.c
      throw new IllegalArgumentException("The user's email address is invalid.");

    // check whether the user exist in the database already; if email exists, the operation fails
    ensureUniqueValue("email", accountMgr.getUserProfileByEmail(email), email, userId);
    return email;
  }

  private static void ensureUniqueValue(String valueName, UserProfile foundProfile, String value, Optional<Long> userId) {
    if (foundProfile != null && (userId.isEmpty() || !userId.get().equals(foundProfile.getUserId()))) {
      // either creating a new user (no found value is acceptable) or editing and value found in different profile
      throw new IllegalArgumentException("The " + valueName + " '" + value + "' is already in use. " + "Please choose another.");
    }
  }

  @Override
  public UserInfo modifyUser(String userIdStr, UserPropertiesRequest userProps) {
    AccountManager accountMgr = new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS);
    Long userId = Long.valueOf(userIdStr);
    validateUserProps(accountMgr, userProps, Optional.of(userId));
    Functions.mapException(
        () -> accountMgr.saveUserProfile(userId, userProps.getEmail(), userProps),
        e -> new RuntimeException(e)); // all exceptions at this point are 500s
    // after saving, read object back out of DB
    UserProfile user = accountMgr.getUserProfile(userId);
    return createUserInfoObject(user, true, DataScope.PROFILE);
  }

  @Override
  public Optional<UserInfo> getGuestProfileInfo(String userId) {
    try {
      // FIXME: since this code directly accesses the DB, it should live in AccountManager;
      //   however that complicates FgpUtil releases prior to move to bearer tokens, so adding it here.
      String sql = "select creation_time from useraccounts.guest_ids where user_id = ?";
      Optional<Date> creationDate = new SQLRunner(_accountDb.getDataSource(), sql, "select-guest")
          .executeQuery(new Object[] { Long.valueOf(userId) }, new Integer[] { Types.BIGINT }, rs ->
              rs.next() ? Optional.of(rs.getDate("creation_time")) : Optional.empty());
      return creationDate.map(date -> {
        UserProfile guest = AccountManager.createGuestProfile("guest", Long.valueOf(userId), date);
        return createUserInfoObject(guest, false, DataScope.PROFILE);
      });
    }
    catch (SQLRunnerException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  @Override
  public void updateLastLoginTimestamp(String userId) {
    AccountManager accountMgr = new AccountManager(_accountDb, _schema, USER_PROPERTY_DEFS);
    accountMgr.updateLastLogin(Long.valueOf(userId));
  }

}
