package org.gusdb.oauth2.eupathdb.accountdb;

import static org.gusdb.fgputil.EncryptionUtil.encryptPassword;
import static org.gusdb.fgputil.FormatUtil.join;
import static org.gusdb.fgputil.functional.Functions.mapToList;
import static org.gusdb.fgputil.functional.Functions.pickKeys;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.FormatUtil;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.db.DBStateException;
import org.gusdb.fgputil.db.SqlUtils;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.db.runner.SQLRunner.ArgumentBatch;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.gusdb.oauth2.client.veupathdb.UserInfo;
import org.gusdb.oauth2.client.veupathdb.UserProperty;

public class AccountDbManager {

  private static final Logger LOG = Logger.getLogger(AccountDbManager.class);

  public static final String TABLE_ACCOUNTS = "accounts";
  public static final String TABLE_ACCOUNT_PROPS = "account_properties";
  public static final String TABLE_SUBSCRIPTION_GROUP_LEADS = "subscription_group_leads";

  private static final String COL_USER_ID = "user_id";
  private static final String COL_EMAIL = "email";
  private static final String COL_PASSWORD = "passwd";
  private static final String COL_IS_GUEST = "is_guest";
  private static final String COL_SIGNATURE = "signature";
  private static final String COL_STABLE_ID = "stable_id";
  private static final String COL_REGISTER_TIME = "register_time";
  private static final String COL_LAST_LOGIN = "last_login";

  // special user property that, if present, users can be looked up by
  public static final String USERNAME_PROPERTY_KEY = "username";

  private static final String COL_PROP_KEY = "key";
  private static final String COL_PROP_VALUE = "value";

  private static final String ACCOUNT_SCHEMA_MACRO = "$$ACCOUNT_SCHEMA$$";
  private static final String DEFINED_PROPERTY_NAMES_MACRO = "$$DEFINED_PROPERTY_NAMES$$";
  private static final String DEFINED_PROPERTY_SELECTION_MACRO = "$$DEFINED_PROPERTIES_MACRO$$";
  private static final String DEFINED_PROPERTY_NAME_MACRO = "$$PROPERTY_NAME$$";
  private static final String EMAIL_LIST_MACRO = "$$EMAIL_LIST$$";
  private static final String ID_LIST_MACRO = "$$ID_LIST$$";

  private static final String INSERT_USER_SQL =
      "insert into " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNTS + " (" + COL_USER_ID +
      "    , " + COL_EMAIL +
      "    , " + COL_PASSWORD +
      "    , " + COL_IS_GUEST +
      "    , " + COL_SIGNATURE +
      "    , " + COL_STABLE_ID +
      "    , " + COL_REGISTER_TIME +
      "    , " + COL_LAST_LOGIN +
      ") values (?, ?, ?, ?, ?, ?, ?, ?)";
  private static final Integer[] INSERT_USER_PARAM_TYPES = {
      Types.BIGINT, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
      Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP, Types.TIMESTAMP
  };

  private static final String INSERT_PROPERTY_SQL = 
      "insert into " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNT_PROPS +
      " (" + COL_USER_ID + "," + COL_PROP_KEY + "," + COL_PROP_VALUE + ") values (?, ?, ?)";
  private static final Integer[] INSERT_PROPERTY_PARAM_TYPES = {
      Types.BIGINT, Types.VARCHAR, Types.VARCHAR
  };

  private static final String REMOVE_PROPERTIES_SQL =
      "delete from " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNT_PROPS + " where " + COL_USER_ID + " = ?";
  private static final Integer[] REMOVE_PROPERTIES_PARAM_TYPES = { Types.BIGINT };

  private static final String SELECT_FLAT_USER_PROPS_SQL =
      "    select " + COL_USER_ID + DEFINED_PROPERTY_SELECTION_MACRO +
      "    from " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNT_PROPS;

  private static final String SELECT_FLAT_USER_SQL =
      "select u." + COL_USER_ID +
      "    , " + COL_EMAIL +
      "    , " + COL_IS_GUEST +
      "    , " + COL_SIGNATURE +
      "    , " + COL_STABLE_ID +
      "    , " + COL_REGISTER_TIME +
      "    , " + COL_LAST_LOGIN + DEFINED_PROPERTY_NAMES_MACRO +
      "  from " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNTS + " u " +
      "  left join (" +
      "    " + SELECT_FLAT_USER_PROPS_SQL +
      "    group by " + COL_USER_ID +
      "  ) p" +
      "  on u." + COL_USER_ID + " = p." + COL_USER_ID;

  private static final String PROPERTY_COLUMN_SELECTION_SQL =
      ", max(case when key = '" + DEFINED_PROPERTY_NAME_MACRO + "' then value end) as " + DEFINED_PROPERTY_NAME_MACRO;

  private static final String USERNAME_CONDITION = "( " + COL_EMAIL + " = ? OR lower(" + USERNAME_PROPERTY_KEY + ") = lower(?) )";

  private static String getUpdateColumnSql(String colName) {
    return "update " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNTS + " set " + colName + " = ? where " + COL_USER_ID + " = ?";
  }

  private static final String UPDATE_PASSWORD_SQL = getUpdateColumnSql(COL_PASSWORD);
  private static final Integer[] UPDATE_PASSWORD_PARAM_TYPES = { Types.VARCHAR, Types.BIGINT };

  private static final String UPDATE_LAST_LOGIN_SQL = getUpdateColumnSql(COL_LAST_LOGIN);
  private static final Integer[] UPDATE_LAST_LOGIN_PARAM_TYPES = { Types.TIMESTAMP, Types.BIGINT };

  private static final String UPDATE_EMAIL_SQL = getUpdateColumnSql(COL_EMAIL);
  private static final Integer[] UPDATE_EMAIL_PARAM_TYPES = { Types.VARCHAR, Types.BIGINT };

  private static final String FIND_USER_IDS_BY_EMAIL_SQL =
      "select " + COL_USER_ID + ", " + COL_EMAIL +
      "  from " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNTS +
      " where " + COL_EMAIL + " in (" + EMAIL_LIST_MACRO + ")";

  private static final String FIND_USER_IDS =
      "select " + COL_USER_ID +
      "  from " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNTS +
      " where " + COL_USER_ID + " in (" + ID_LIST_MACRO + ")";

  private static final String DELETE_USER_PROPERTIES =
      "delete from " + ACCOUNT_SCHEMA_MACRO + TABLE_ACCOUNT_PROPS +
      " where " + COL_USER_ID + " = ?";

  private static final String DELETE_USER_GROUP_LEADS =
      "delete from " + ACCOUNT_SCHEMA_MACRO + TABLE_SUBSCRIPTION_GROUP_LEADS +
      " where " + COL_USER_ID + " = ?";

  private final DatabaseInstance _accountDb;
  private final String _accountSchema;
  private final Map<String, UserProperty> _propertyNames = new LinkedHashMap<>();
  private final String _selectSql;

  public AccountDbManager(DatabaseInstance accountDb, String accountSchema, List<UserProperty> propertyNames) {
    _accountDb = accountDb;
    _accountSchema = accountSchema;
    for (UserProperty prop : propertyNames) {
      _propertyNames.put(prop.getName(), prop);
    }
    _selectSql = getSelectSql(_accountSchema, propertyNames);
  }

  private static String getSelectSql(String schema, List<UserProperty> propertyNames) {
    return SELECT_FLAT_USER_SQL
        .replace(ACCOUNT_SCHEMA_MACRO, schema)
        .replace(DEFINED_PROPERTY_NAMES_MACRO, join(mapToList(propertyNames,
            prop -> ", " + prop.getDbKey()).toArray(), ""))
        .replace(DEFINED_PROPERTY_SELECTION_MACRO, getPropSelectionSql(propertyNames));
  }

  private static String getPropSelectionSql(List<UserProperty> propertyNames) {
    return join(mapToList(propertyNames, prop ->
      PROPERTY_COLUMN_SELECTION_SQL.replace(DEFINED_PROPERTY_NAME_MACRO, prop.getDbKey()
    )).toArray(), "");
  }

  public UserProfile getUserProfile(Long userId) {
    // note: need to qualify user_id column with 'u.' because of join above; not needed for other fields
    return getSingleUserProfile(" where u." + COL_USER_ID + " = ?",
        new Object[] { userId },
        new Integer[] { Types.INTEGER });
  }

  public static String trimAndLowercase(String usernameOrEmail) {
    return usernameOrEmail.trim().toLowerCase();
  }

  public UserProfile getUserProfileByUsername(String username) {
    return getSingleUserProfile(" where lower(" + USERNAME_PROPERTY_KEY + ") = lower(?)",
        new Object[] { username.trim() },
        new Integer[] { Types.VARCHAR });
  }

  public UserProfile getUserProfileByEmail(String email) {
    return getSingleUserProfile(" where " + COL_EMAIL + " = ?",
        new Object[] { trimAndLowercase(email) },
        new Integer[] { Types.VARCHAR });
  }

  public UserProfile getUserProfileByUsernameOrEmail(String usernameOrEmail) {
    String trimmedUserNameOrEmail = trimAndLowercase(usernameOrEmail);
    return getSingleUserProfile(" where " + USERNAME_CONDITION,
        new Object[] { trimmedUserNameOrEmail, trimmedUserNameOrEmail },
        new Integer[] { Types.VARCHAR, Types.VARCHAR });
  }

  public UserProfile getUserProfile(String usernameOrEmail, String password) {
    String trimmedUsernameOrEmail = trimAndLowercase(usernameOrEmail);
    return getSingleUserProfile(" where " + USERNAME_CONDITION + " and " + COL_PASSWORD + " = ?",
        new Object[] { trimmedUsernameOrEmail, trimmedUsernameOrEmail, encryptPassword(password) },
        new Integer[] { Types.VARCHAR, Types.VARCHAR, Types.VARCHAR });
  }

  public UserProfile getUserProfileBySignature(String signature) {
    return getSingleUserProfile(" where " + COL_SIGNATURE + " = ?",
        new Object[] { signature },
        new Integer[] { Types.VARCHAR });
  }

  private UserProfile getSingleUserProfile(final String condition, final Object[] params, final Integer[] types) {
    String sql = new StringBuilder(_selectSql).append(condition).toString();
    LOG.debug("Running the following SQL: " + sql);
    return new SQLRunner(_accountDb.getDataSource(), sql).executeQuery(params, types, rs -> {
      if (rs.next()) {
        UserProfile profile = loadUserProfile(rs, _propertyNames.values());
        if (rs.next()) {
          throw new IllegalStateException("More than one user found under condition '" +
              condition + "' with values: " + FormatUtil.join(params, ", "));
        }
        return profile;
      }
      return null;  
    });
  }

  private static UserProfile loadUserProfile(ResultSet rs, Collection<UserProperty> props) throws SQLException {
    UserProfile profile = new UserProfile();
    profile.setUserId(rs.getLong(COL_USER_ID));
    profile.setEmail(rs.getString(COL_EMAIL));
    profile.setGuest(rs.getBoolean(COL_IS_GUEST));
    profile.setSignature(rs.getString(COL_SIGNATURE));
    profile.setStableId(rs.getString(COL_STABLE_ID));
    profile.setRegisterTime(rs.getDate(COL_REGISTER_TIME));
    profile.setLastLoginTime(rs.getDate(COL_LAST_LOGIN));
    Map<String, String> properties = new HashMap<>();
    for (UserProperty prop : props) {
      String value = rs.getString(prop.getDbKey());
      if (!rs.wasNull()) {
        properties.put(prop.getName(), value);
      }
    }
    profile.setProperties(properties);
    LOG.debug("Loaded profile: " + profile);
    return profile;
  }

  public UserProfile createAccount(String email, String password, Map<String, String> profileProperties) throws Exception {

    // make sure email is trimmed/lowercase
    email = trimAndLowercase(email);

    // make sure username is non-empty after trimming and remove if so
    massageUsername(profileProperties);

    // assign user ID to the new user (unique, stable, sequential, primary key identifier)
    long userId = getNextUserId();

    // generate signature for this user (unique, stable, anonymous identifier)
    String signature = encryptPassword(userId + "_" + email);

    // generate stable ID for this user (unique, stable, human-readable identifier)
    String stableId = createStableId(email, userId);

    // save new user account to DB
    persistNewAccount(userId, email, password, signature, stableId, profileProperties, false);

    // read user profile back out of the database
    return getUserProfile(userId);
  }

  /**
   * make sure trimmed username is non-empty; if empty, remove
   */
  private void massageUsername(Map<String, String> profileProperties) {
    if (profileProperties.containsKey(USERNAME_PROPERTY_KEY)) {
      String trimmedValue = profileProperties.get(USERNAME_PROPERTY_KEY).trim();
      if (trimmedValue.isEmpty()) {
        profileProperties.remove(USERNAME_PROPERTY_KEY);
      }
      else {
        profileProperties.put(USERNAME_PROPERTY_KEY, trimmedValue);
      }
    }
  }

  private void persistNewAccount(long userId, String email, String password, String signature,
      String stableId, Map<String, String> profileProperties, boolean assignLastLogin) throws Exception {
    // define SQL and params to insert user row
    Timestamp now = new Timestamp(new Date().getTime());
    final String insertUserSql = INSERT_USER_SQL.replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    final Object[] params = { userId, email, encryptPassword(password), false,
        signature, stableId, now, assignLastLogin ? now : null };

    // define SQL and params to insert property rows
    final String insertPropSql = INSERT_PROPERTY_SQL.replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    final ArgumentBatch propertyBatch = getUserPropertyBatch(userId, profileProperties);

    // perform all inserts in a transaction
    SqlUtils.performInTransaction(_accountDb.getDataSource(), conn -> {
      // perform user row insert
      new SQLRunner(conn, insertUserSql, "insert-user-row").executeStatement(params, INSERT_USER_PARAM_TYPES);
      // perform property rows insert
      new SQLRunner(conn, insertPropSql, "insert-user-prop-rows").executeStatementBatch(propertyBatch);
    });
  }

  private long getNextUserId() throws DBStateException, SQLException {
    return _accountDb.getPlatform().getNextId(_accountDb.getDataSource(), _accountSchema, TABLE_ACCOUNTS);
  }

  private ArgumentBatch getUserPropertyBatch(final long userId, Map<String, String> profileProperties) {
    // deal with null property map; this can sometimes be passed
    if (profileProperties == null) profileProperties = Collections.EMPTY_MAP;
    // first trim props to those allowed by the configuration of this account manager
    final Set<String> propKeys = _propertyNames.keySet();
    final Map<String,String> trimmedProps = pickKeys(profileProperties, propKey -> propKeys.contains(propKey));
    return new ArgumentBatch() {

      @Override
      public Iterator<Object[]> iterator() {
        return IteratorUtil.transform(trimmedProps.entrySet().iterator(), property ->
            new Object[] { userId, _propertyNames.get(property.getKey()).getDbKey(), property.getValue() });
      }

      @Override
      public int getBatchSize() {
        return trimmedProps.size();
      }

      @Override
      public Integer[] getParameterTypes() {
        return INSERT_PROPERTY_PARAM_TYPES;
      }
    };
  }

  private static String createStableId(String email, long userId) {
    int atSignIndex = email.indexOf('@');
    if (atSignIndex == -1) atSignIndex = email.length();
    return email.substring(0, atSignIndex).toLowerCase() + "." + userId;
  }

  /**
   * Creates a temporary user profile for a guest user.  Guest users are not persisted
   * in the AccountDB, but this mechanism ensures user IDs are not duplicated or
   * conflicting across various sites that use AccountDB.
   * 
   * @param stableIdPrefix string indicating type of temporary user (e.g. true guest or system user)
   * @return user profile for the guest
   * @throws SQLException if unable to allocate a new ID for this user
   */
  public UserProfile createGuestAccount(String stableIdPrefix) throws SQLException {
    long userId = getNextUserId();
    return createGuestProfile(stableIdPrefix, userId, new Date());
  }

  /**
   * Creates a complete user profile from a stable ID prefix, user ID, and timestamp.
   * 
   * @param stableIdPrefix
   * @param userId
   * @param timestamp
   * @return 
   */
  public static UserProfile createGuestProfile(String stableIdPrefix, long userId, Date timestamp) {
    String stableId = stableIdPrefix + userId;
    UserProfile profile = new UserProfile();
    profile.setUserId(userId);
    profile.setGuest(true);
    profile.setEmail(stableId);
    profile.setStableId(stableId);
    profile.setSignature(encryptPassword(stableId));
    profile.setRegisterTime(timestamp);
    profile.setLastLoginTime(timestamp);
    profile.setProperties(Collections.EMPTY_MAP);
    return profile;
  }

  private void updateColumn(String rawSql, Integer[] argTypes, String queryName, long userId, Object newValue) {
    String sql = rawSql.replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    new SQLRunner(_accountDb.getDataSource(), sql, queryName)
      .executeUpdate(new Object[]{ newValue, userId }, argTypes);
  }

  public void updatePassword(long userId, String newPassword) {
    updateColumn(UPDATE_PASSWORD_SQL, UPDATE_PASSWORD_PARAM_TYPES,
        "update-user-password", userId, encryptPassword(newPassword));
  }

  public void updateLastLogin(long userId) {
    updateColumn(UPDATE_LAST_LOGIN_SQL, UPDATE_LAST_LOGIN_PARAM_TYPES,
        "update-user-last-login", userId, new Timestamp(new Date().getTime()));
  }

  public void saveUserProfile(final long userId, String email, Map<String, String> profileProperties) throws Exception {

    // first update email; this can be done independently of property updates
    updateColumn(UPDATE_EMAIL_SQL, UPDATE_EMAIL_PARAM_TYPES,
        "update-user-email", userId, email.trim().toLowerCase());

    // make sure trimmed username is non-empty; if empty, remove
    massageUsername(profileProperties);

    // define SQL and params to remove existing property rows and replace with new via insert
    final String removePropsSql = REMOVE_PROPERTIES_SQL.replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    final Object[] removePropsParams = { userId };
    final String insertPropSql = INSERT_PROPERTY_SQL.replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    final ArgumentBatch propertyBatch = getUserPropertyBatch(userId, profileProperties);

    // perform all property-related operations in a transaction
    SqlUtils.performInTransaction(_accountDb.getDataSource(), conn -> {
      // perform property rows delete
      new SQLRunner(conn, removePropsSql, "remove-user-prop-rows").executeStatement(removePropsParams, REMOVE_PROPERTIES_PARAM_TYPES);
      // perform property rows insert
      new SQLRunner(conn, insertPropSql, "insert-user-prop-rows").executeStatementBatch(propertyBatch);
    });
  }

  public static String getFlatPropertySql(List<UserProperty> propertyNames, String accountSchema, String accountDbLink) {
    return (SELECT_FLAT_USER_PROPS_SQL + accountDbLink)
        .replace(ACCOUNT_SCHEMA_MACRO, accountSchema)
        .replace(DEFINED_PROPERTY_SELECTION_MACRO, getPropSelectionSql(propertyNames));
  }

  public Map<String,Long> lookUpUserIdsByEmail(Collection<String> emailList) {
    String emailListSql = emailList.stream().map(id -> "'" + id.replace("'", "''") + "'").collect(Collectors.joining(","));
    String sql = FIND_USER_IDS_BY_EMAIL_SQL
        .replace(ACCOUNT_SCHEMA_MACRO, _accountSchema)
        .replace(EMAIL_LIST_MACRO, emailListSql);
    return new SQLRunner(_accountDb.getDataSource(), sql, "look-up-user-ids-by-email").executeQuery(rs -> {
      Map<String, Long> result = new HashMap<>();
      while (rs.next()) {
        result.put(rs.getString(COL_EMAIL), rs.getLong(COL_USER_ID));
      }
      return result;
    });
  }

  public Map<Long,Boolean> verifyUserids(Collection<Long> userIdList) {
    String sql = FIND_USER_IDS
        .replace(ACCOUNT_SCHEMA_MACRO, _accountSchema)
        .replace(ID_LIST_MACRO, join(userIdList, ","));
    Map<Long, Boolean> result = new SQLRunner(_accountDb.getDataSource(), sql, "find-user-ids")
      .executeQuery(rs -> {
        Map<Long, Boolean> result1 = new HashMap<>();
        while (rs.next()) {
          result1.put(rs.getLong(COL_USER_ID), true);
        }
        return result1;
      });
    for (Long id : userIdList) {
      if (!result.containsKey(id)) {
        result.put(id, false);
      }
    }
    return result;
  }

  public void anonymizeUser(Long userId) {

    // delete all user's account properties
    String propsSql = DELETE_USER_PROPERTIES
        .replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    new SQLRunner(_accountDb.getDataSource(), propsSql, "delete-user-props")
      .executeStatement(new Object[] { userId }, new Integer[] { Types.BIGINT });

    // put back first_name and last_name with stub values
    for (Entry<String,String> propUpdate : List.of(
        new TwoTuple<>(UserInfo.FIRST_NAME_PROP_KEY, "deleted-user"),
        new TwoTuple<>(UserInfo.LAST_NAME_PROP_KEY, userId.toString())
    )) {
      propsSql = INSERT_PROPERTY_SQL
          .replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
      new SQLRunner(_accountDb.getDataSource(), propsSql, "modify-prop-for-deletion")
          .executeStatement(new Object[] { userId, propUpdate.getKey(), propUpdate.getValue() }, INSERT_PROPERTY_PARAM_TYPES);
    }

    // delete user as subscription group leads
    String leadsSql = DELETE_USER_GROUP_LEADS
        .replace(ACCOUNT_SCHEMA_MACRO, _accountSchema);
    new SQLRunner(_accountDb.getDataSource(), leadsSql, "delete-user-as-group-leads")
      .executeStatement(new Object[] { userId }, new Integer[] { Types.BIGINT });

    // modify user props to anonymize and prevent future login or password reset
    for (Entry<String,String> columnUpdate : List.of(
        new TwoTuple<>(COL_EMAIL, "deleted-user." + userId + "@veupathdb.org"),
        new TwoTuple<>(COL_PASSWORD, ""),
        new TwoTuple<>(COL_STABLE_ID, "deleted-user." + userId)
    )) {
      new SQLRunner(_accountDb.getDataSource(), getUpdateColumnSql(columnUpdate.getKey()), "modify-col-for-deletion")
          .executeStatement(new Object[] { columnUpdate.getValue(), userId }, new Integer[] { Types.VARCHAR, Types.BIGINT });
    }
  }
}
