package org.gusdb.oauth2.eupathdb;

import static org.gusdb.fgputil.FormatUtil.getInnerClassLog4jName;
import static org.gusdb.oauth2.client.veupathdb.UserInfo.USER_PROPERTY_LIST;

import java.net.URI;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.ConnectionPoolConfig;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunnerException;
import org.gusdb.fgputil.db.slowquery.QueryLogConfig;
import org.gusdb.fgputil.db.slowquery.QueryLogger;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.oauth2.Authenticator;
import org.gusdb.oauth2.InitializationException;
import org.gusdb.oauth2.UserAccountInfo;
import org.gusdb.oauth2.client.veupathdb.UserInfo;
import org.gusdb.oauth2.client.veupathdb.UserProperty;
import org.gusdb.oauth2.client.veupathdb.UserProperty.InputType;
import org.gusdb.oauth2.eupathdb.accountdb.AccountDbManager;
import org.gusdb.oauth2.eupathdb.accountdb.UserProfile;
import org.gusdb.oauth2.exception.ConflictException;
import org.gusdb.oauth2.exception.InvalidPropertiesException;
import org.gusdb.oauth2.service.UserPropertiesRequest;

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
    schema,
    adminUserIds
  }

  private DatabaseInstance _accountDb;
  private String _schema;
  private List<String> _adminUserIds;

  public AccountDbInfo getAccountDbInfo() {
    return new AccountDbInfo(_accountDb, _schema);
  }

  @Override
  public List<String> getAdminUserIds() { return _adminUserIds; }

  @Override
  public void initialize(JsonObject configJson) throws InitializationException {
    ConnectionPoolConfig dbConfig = SimpleDbConfig.create(
        SupportedPlatform.toPlatform(configJson.getString(JsonKey.platform.name())),
        configJson.getString(JsonKey.connectionUrl.name()),
        configJson.getString(JsonKey.login.name()),
        configJson.getString(JsonKey.password.name()),
        (short)configJson.getInt(JsonKey.poolSize.name()));
    String schema = configJson.getString(JsonKey.schema.name());
    List<String> adminUserIds = List.of(configJson.getString(JsonKey.adminUserIds.name(), "").split(","));
    initialize(dbConfig, schema, adminUserIds);
    QueryLogger.initialize(new QueryLogConfig(){}); // use defaults
  }

  protected void initialize(ConnectionPoolConfig dbConfig, String schema, List<String> adminUserIds) {
    LOG.info("Initializing database using: " + dbConfig);
    _accountDb = new DatabaseInstance(dbConfig, true);
    if (!schema.isEmpty() && !schema.endsWith(".")) schema += ".";
    _schema = schema;
    _adminUserIds = adminUserIds;
  }

  // WDK uses email and password
  @Override
  public Optional<String> isCredentialsValid(String username, String password) throws Exception {
    return Optional.ofNullable(
        new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).getUserProfile(username, password))
      .map(profile -> profile.getUserId().toString());
  }

  @Override
  public Optional<UserAccountInfo> getUserInfoByLoginName(String loginName, DataScope scope) throws Exception {
    return getUserInfo(new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).getUserProfileByUsernameOrEmail(loginName), scope);
  }

  @Override
  public Optional<UserAccountInfo> getUserInfoByUserId(String userId, DataScope scope) throws Exception {
    return getUserInfo(new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).getUserProfile(Long.valueOf(userId)), scope);
  }

  @Override
  public void resetPassword(String userId, String newPassword) {
    new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).updatePassword(Long.valueOf(userId), newPassword);
  }

  @Override
  public String generateNewPassword() {
    return generateRandomChars(12);
  }

  public static String generateRandomChars(int numChars) {
    // generate a random password of 12 characters, each in the range [0-9A-Za-z]
    StringBuilder buffer = new StringBuilder();
    Random rand = new Random();
    for (int i = 0; i < numChars; i++) {
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

  Optional<UserAccountInfo> getUserInfo(UserProfile profile, DataScope scope) {
    return Optional.ofNullable(profile).map(p -> createUserInfoObject(p, true, scope));
  }

  private UserAccountInfo createUserInfoObject(UserProfile profile, boolean isEmailVerified, DataScope scope) {
    return new UserAccountInfo() {
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
        JsonObjectBuilder builder = Json.createObjectBuilder();
        Map<String,String> props = profile.getProperties();
        switch (scope) {
          case PROFILE:
            for (String propName : UserInfo.USER_PROPERTIES.keySet()) {
              builder.add(propName, Optional.ofNullable(props.get(propName)).orElse(""));
            }
            break;
          case ID_TOKEN:
            builder.add("name", UserInfo.formatDisplayName(
                props.get("firstName"),
                props.get("middleName"),
                props.get("lastName")
            ));
            builder.add("organization", props.get("organization"));
            break;
          case BEARER_TOKEN:
          default:
            return Collections.emptyMap();
        }
        // convert JSON object to map of String -> JsonValue
        return builder.build().entrySet().stream()
            .peek(entry -> LOG.debug("Adding entry to supplemental fields: " + entry.getKey() + " -> " + entry.getValue().toString()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

      }
    };
  }

  // protected so TestAuthenticator can override
  protected UserProfile getUserProfile(String userId) {
    return new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).getUserProfile(Long.valueOf(userId));
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
      String id = new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).createGuestAccount("guest_").getUserId().toString();
      // FIXME: since this code directly accesses the DB, it should live in AccountManager;
      //   however that complicates FgpUtil releases prior to move to bearer tokens, so adding it here.
      String sql = "insert into useraccounts.guest_ids (user_id, creation_time) values (?, current_date)";
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
    new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST).updatePassword(profile.getUserId(), newPassword);
  }

  @Override
  public JsonValue executeQuery(JsonObject querySpec)
      throws UnsupportedOperationException, IllegalArgumentException {
    AccountDbManager acctDb = new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST);
    return new UserQueryHandler(this, acctDb).handleQuery(querySpec);
  }

  @Override
  public void logSuccessfulLogin(String loginName, String userId, String clientId, String redirectUri, String requestingIpAddress) {
    LOGIN_LOG.info(requestingIpAddress + " " + clientId + " " + getHost(redirectUri) + " " + userId);
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
  public UserAccountInfo createUser(UserPropertiesRequest userProps, String initialPassword) throws ConflictException, InvalidPropertiesException {
    AccountDbManager accountMgr = new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST);
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
   * @throws ConflictException 
   * @throws InvalidPropertiesException 
   */
  private void validateUserProps(AccountDbManager accountMgr, UserPropertiesRequest userProps, Optional<Long> userId) throws ConflictException, InvalidPropertiesException {

    // check email for uniqueness and format
    userProps.setEmail(validateAndFormatEmail(userProps.getEmail(), accountMgr, userId));

    // if user supplied a username, make sure it is unique
    if (userProps.containsKey(AccountDbManager.USERNAME_PROPERTY_KEY)) {
      String username = userProps.get(AccountDbManager.USERNAME_PROPERTY_KEY);
      // check whether the username exists in the database already under a different user
      ensureUniqueValue("username", accountMgr.getUserProfileByUsername(username), username, userId);
    }

    // property validation
    for (UserProperty prop : UserInfo.USER_PROPERTIES.values()) {
      String value = userProps.get(prop.getName());
      boolean propPopulated = value != null && !value.isBlank();

      // make sure required props are present; trimming of unsupported keys is done in AccountManager
      if (prop.isRequired() && !propPopulated) {
        throw new InvalidPropertiesException("User property '" + prop.getName() + "' cannot be empty.");
      }

      // check select box values against vocabs used to populate the boxes
      if (prop.getInputType() == InputType.SELECT && propPopulated) {
        List<String> vocab = UserPropertyVocabulary.VOCAB_MAP.get(prop.getName());
        if (!vocab.contains(value)) {
          throw new InvalidPropertiesException("User property '" + prop.getName() +
              "' must have one of the following values: [" + String.join(", ", vocab) + "]");
        }
      }
    }
  }

  private static String validateAndFormatEmail(String email, AccountDbManager accountMgr, Optional<Long> userId) throws InvalidPropertiesException, ConflictException {
    // trim and validate passed email address and extract stable name
    if (email == null)
      throw new InvalidPropertiesException("The user's email cannot be empty.");
    // format the info
    email = AccountDbManager.trimAndLowercase(email);
    if (email.isEmpty())
      throw new InvalidPropertiesException("The user's email cannot be empty.");
    int atSignIndex = email.indexOf("@");
    if (atSignIndex < 1) // must be present and not the first char
      throw new InvalidPropertiesException("The user's email address is invalid.");
    if (atSignIndex > email.length() - 4) // must be at least a@b.c
      throw new InvalidPropertiesException("The user's email address is invalid.");

    // check whether the user exist in the database already; if email exists, the operation fails
    ensureUniqueValue("email", accountMgr.getUserProfileByEmail(email), email, userId);
    return email;
  }

  private static void ensureUniqueValue(String valueName, UserProfile foundProfile, String value, Optional<Long> userId) throws ConflictException {
    if (foundProfile != null && (userId.isEmpty() || !userId.get().equals(foundProfile.getUserId()))) {
      // either creating a new user (no found value is acceptable) or editing and value found in different profile
      throw new ConflictException("The " + valueName + " '" + value + "' is already in use. " + "Please choose another.");
    }
  }

  @Override
  public UserAccountInfo modifyUser(String userIdStr, UserPropertiesRequest userProps) throws ConflictException, InvalidPropertiesException {
    AccountDbManager accountMgr = new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST);
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
  public void deleteUser(String userIdStr) {
    AccountDbManager accountMgr = new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST);
    Long userId = Long.valueOf(userIdStr);
    accountMgr.anonymizeUser(userId);
  }

  @Override
  public Optional<UserAccountInfo> getGuestProfileInfo(String userId) {
    try {
      // FIXME: since this code directly accesses the DB, it should live in AccountManager;
      //   however that complicates FgpUtil releases prior to move to bearer tokens, so adding it here.
      String sql = "select creation_time from useraccounts.guest_ids where user_id = ?";
      Optional<Date> creationDate = new SQLRunner(_accountDb.getDataSource(), sql, "select-guest")
          .executeQuery(new Object[] { Long.valueOf(userId) }, new Integer[] { Types.BIGINT }, rs ->
              rs.next() ? Optional.of(rs.getDate("creation_time")) : Optional.empty());
      return creationDate.map(date -> {
        UserProfile guest = AccountDbManager.createGuestProfile("guest", Long.valueOf(userId), date);
        return createUserInfoObject(guest, false, DataScope.PROFILE);
      });
    }
    catch (SQLRunnerException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  @Override
  public void updateLastLoginTimestamp(String userId) {
    AccountDbManager accountMgr = new AccountDbManager(_accountDb, _schema, USER_PROPERTY_LIST);
    accountMgr.updateLastLogin(Long.valueOf(userId));
  }

}
