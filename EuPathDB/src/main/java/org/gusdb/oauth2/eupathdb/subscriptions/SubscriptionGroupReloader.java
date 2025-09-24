package org.gusdb.oauth2.eupathdb.subscriptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.MapBuilder;
import org.gusdb.fgputil.db.platform.DBPlatform;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.functional.FunctionalInterfaces.Reducer;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.json.JSONArray;
import org.json.JSONObject;

@Deprecated
public class SubscriptionGroupReloader {

  private static final Logger LOG = LogManager.getLogger(SubscriptionGroupReloader.class);

  public static void main(String[] args) throws Exception {

    // configuration for command line tool
    final boolean PRINT_GROUP_DETAILS = false;
    final boolean WRITE_TO_DB = false;

    // assumptions made for the command line version
    final SupportedPlatform PLATFORM = SupportedPlatform.ORACLE;
    final String ACCOUNT_SCHEMA = "useraccounts.";

    if (args.length != 4) {
      System.err.println("USAGE: AcctDbDumpReader <annotated_accounts_file> <db_connection_string> <db_user> <db_password>");
      System.exit(1);
    }
    Path accountsFile = Paths.get(args[0]);
    String dbConnectionString = args[1];
    String dbUser = args[2];
    String dbPass = args[3];
    SimpleDbConfig dbConfig = SimpleDbConfig.create(PLATFORM, dbConnectionString, dbUser, dbPass);
    LOG.info("Opening connection to database");
    try (DatabaseInstance dbInstance = new DatabaseInstance(dbConfig)) {
      JSONObject result = new SubscriptionGroupReloader(dbInstance.getDataSource(), PLATFORM.getPlatformInstance(), ACCOUNT_SCHEMA)
          .loadSubscriptions(accountsFile, PRINT_GROUP_DETAILS, WRITE_TO_DB);
      System.out.println(result.toString(2));
      LOG.info("Closing connection to DB");
    }
    LOG.info("Done");
  }

  private static class Group {

    private static long SUBSCRIPTION_ID_SEQ = 0;
    private static long GROUP_ID_SEQ = 0;

    public final long subscriptionId;
    public final long groupId;
    public final String subscriptionToken = randomAlphaNumericString(10);
    public final List<Long> memberUserIds = new ArrayList<>();
    public final List<Long> leadUserIds = new ArrayList<>();
    public String name;
    public boolean hasInvoice;

    public Group(DataSource ds) { //throws SQLException {
      subscriptionId = SUBSCRIPTION_ID_SEQ += 10;
      groupId = GROUP_ID_SEQ += 10;
      // this takes way too long; use memory instead
      //subscriptionId = PLATFORM.getPlatformInstance().getNextId(ds, ACCOUNT_SCHEMA, "subscriptions");
      //groupId = PLATFORM.getPlatformInstance().getNextId(ds, ACCOUNT_SCHEMA, "subscription_groups");
    }

    public JSONObject toJson() {
      return new JSONObject()
          .put("groupId",groupId)
          .put("subscriptionToken", subscriptionToken)
          .put("name", name)
          .put("hasInvoice", hasInvoice)
          .put("numMembers", memberUserIds.size())
          .put("memberUserIds", new JSONArray(memberUserIds))
          .put("leadUserIds", new JSONArray(leadUserIds));
    }
  }

  // for use with randomAlphaNumericString
  private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
  private static final String RANDOM_CHARS = "0123456789" + ALPHA + ALPHA.toUpperCase();

  // copied from WDK's Utilities.java
  private static String randomAlphaNumericString(int numChars) {
    StringBuilder str = new StringBuilder();
    new Random()
      .ints(numChars, 0, RANDOM_CHARS.length())
      .forEach(i -> str.append(RANDOM_CHARS.charAt(i)));
    return str.toString();
  }

  private static final String SUBSCRIPTION_TOKEN_KEY = "subscription_token";

  private static final int USERID_INDEX = 0;
  private static final int GROUPCLEAN_INDEX = 13;
  private static final int HASINVOICE_INDEX = 15;
  private static final int ISLEAD_INDEX = 16;

  private static final int REQUIRED_NUM_COLS = ISLEAD_INDEX + 1;

  private final DataSource _acctDbDs;
  private final DBPlatform _acctDbPlatform;
  private final String _accountsSchema;

  public SubscriptionGroupReloader(DataSource acctDbDs, DBPlatform acctDbPlatform, String accountsSchema) {
    _acctDbDs = acctDbDs;
    _acctDbPlatform = acctDbPlatform;
    _accountsSchema = accountsSchema;
  }

  public JSONObject loadSubscriptions(Path accountsFile, boolean returnGroupDetails, boolean writeToDb) throws IOException {

    // JSON Object will hold both observations and warnings
    JSONObject result = new JSONObject()
        .put("columns", new JSONObject()
            .put("requiredColumns", new JSONArray()
                .put(new JSONObject().put("index", USERID_INDEX).put("column", "userid"))
                .put(new JSONObject().put("index", GROUPCLEAN_INDEX).put("column", "groupclean"))
                .put(new JSONObject().put("index", HASINVOICE_INDEX).put("column", "hasinvoice"))
                .put(new JSONObject().put("index", ISLEAD_INDEX).put("column", "islead"))));

    // data structure for the groups (key = group_clean aka name)
    Map<String, Group> groups = new LinkedHashMap<>();

    List<String> lineWarnings = new ArrayList<>();
    List<String> userWarnings = new ArrayList<>();
    List<String> groupWarnings = new ArrayList<>();
    List<String> groupLeadWarnings = new ArrayList<>();

    // first load accounts file
    LOG.info("Opening accounts file");
    try (BufferedReader in = new BufferedReader(new FileReader(accountsFile.toFile(), StandardCharsets.UTF_8))) {
      String[] headerTokens = in.readLine().split("\t"); // read header

      JSONArray discoveredColumns = new JSONArray();
      result.getJSONObject("columns").put("discoveredColumns", discoveredColumns);
      for (int i = 0; i < headerTokens.length; i++) {
        discoveredColumns.put(new JSONObject().put("index", i).put("column", headerTokens[i]));
      }

      Set<Long> inconsistentGroups = new HashSet<>();

      while (in.ready()) {

        // read data in the row, skipping rows without enough columns
        String[] tokens = in.readLine().split("\t");
        if (tokens.length < REQUIRED_NUM_COLS) {
          lineWarnings.add(tokens.length == 0 ? "Skipping empty line" : "Line for user " + tokens[0] + " does not have enough columns.");
          continue;
        }

        // get the columns we care about
        Long userId = Long.parseLong(tokens[USERID_INDEX].trim());
        String groupClean = tokens[GROUPCLEAN_INDEX].trim();
        boolean hasInvoice = tokens[HASINVOICE_INDEX].trim().equals("Yes");
        boolean isLead = tokens[ISLEAD_INDEX].trim().equals("Yes");

        if (groupClean.isEmpty()) {
          if (hasInvoice || isLead) {
            userWarnings.add("User " + userId + " does not have a group_clean but has Yes in the invoice (" + hasInvoice + ") or lead (" + isLead + ") column.");
          }
          continue; // nothing to update
        }

        // try to find existing group
        Group group = groups.get(groupClean);
        if (group != null) {
          // perform checks
          if (group.hasInvoice != hasInvoice && !inconsistentGroups.contains(group.groupId)) {
            groupWarnings.add("Group '" + groupClean + "' has inconsistent hasInvoice information.");
            // if any group has hasInvoice=true, set to true
            group.hasInvoice = true;
            inconsistentGroups.add(group.groupId);
          }
        }
        else {
          // make a new group
          group = new Group(_acctDbDs);
          group.name = groupClean;
          group.hasInvoice = hasInvoice;
          groups.put(group.name, group);
        }

        // add user to group
        group.memberUserIds.add(userId);

        // set lead if noted
        if (isLead) {
          group.leadUserIds.add(userId);
        }
      }
    }

    // all data loaded for groups; print additional warnings before printing groups
    for (Group group : groups.values()) {
      if (group.leadUserIds.isEmpty()) {
        groupLeadWarnings.add("Group '" + group.name + "' does not have any leads.");
      }
    }

    result.put("warnings", new JSONObject()
        .put("lineWarnings", lineWarnings)
        .put("userWarnings", userWarnings)
        .put("groupLeadWarnings", groupLeadWarnings)
        .put("groupWarnings", groupWarnings));

    String[] sortedNames = new ArrayList<>(groups.keySet()).toArray(new String[0]);
    Arrays.sort(sortedNames);
    result.put("sortedGroupNames", new JSONArray(sortedNames));

    if (returnGroupDetails) {
      JSONArray groupsJson = new JSONArray();
      for (Group group : groups.values()) {
        groupsJson.put(group.toJson());
      }
      result.put("groupDetails", groupsJson);
    }

    if (writeToDb) {

      JSONObject tableCounts = new JSONObject();
      result.put("tableCounts", tableCounts);

      // first clear out the old data (without disturbing the schema)
      Map<String,String> cleanCommands = new MapBuilder<String,String>(new LinkedHashMap<>())
          .put("account_properties", "DELETE FROM " + _accountsSchema + "ACCOUNT_PROPERTIES WHERE KEY = 'subscription_token'")
          .put("subscription_group_leads", "DELETE FROM " + _accountsSchema + "SUBSCRIPTION_GROUP_LEADS")
          .put("subscription_groups", "DELETE FROM " + _accountsSchema + "SUBSCRIPTION_GROUPS")
          .put("invoices", "DELETE FROM " + _accountsSchema + "INVOICES")
          .put("subscriptions", "DELETE FROM " + _accountsSchema + "SUBSCRIPTIONS")
          .toMap();
      for (Entry<String,String> tableToSql : cleanCommands.entrySet()) {
        tableCounts.put(tableToSql.getKey(), new JSONObject().put("before",
            new SQLRunner(_acctDbDs, tableToSql.getValue()).executeUpdate()));
      }

      // insert subscription records (one for each group for now since we are only reading the accounts file)
      String subscriptionInsertSql = "insert into " + _accountsSchema + "subscriptions (subscription_id, is_active) values (?, ?)";
      Iterator<Object[]> subIter = IteratorUtil.transform(groups.values().iterator(),
          group -> new Object[] { group.subscriptionId, group.hasInvoice });
      LOG.info("Inserting " + groups.size() + " rows into subscriptions table");
      tableCounts.getJSONObject("subscriptions").put("after", groups.size());
      new SQLRunner(_acctDbDs, subscriptionInsertSql).executeStatementBatch(new BasicArgumentBatch() {
        @Override public Iterator<Object[]> iterator() { return subIter; }
      }
      .setBatchSize(groups.size())
      .setParameterTypes(new Integer[] { Types.BIGINT, _acctDbPlatform.getBooleanType() }));

      // insert group records
      String groupInsertSql = "insert into " + _accountsSchema + "subscription_groups (group_id, subscription_id, group_name, subscription_token) values (?, ?, ?, ?)";
      Iterator<Object[]> groupIter = IteratorUtil.transform(groups.values().iterator(),
          group -> new Object[] { group.groupId, group.subscriptionId, group.name, group.subscriptionToken });
      LOG.info("Inserting " + groups.size() + " rows into subscription_groups table");
      tableCounts.getJSONObject("subscription_groups").put("after", groups.size());
      new SQLRunner(_acctDbDs, groupInsertSql).executeStatementBatch(new BasicArgumentBatch() {
        @Override public Iterator<Object[]> iterator() { return groupIter; }
      }
      .setBatchSize(groups.size())
      .setParameterTypes(new Integer[] { Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.VARCHAR }));

      // insert group leads
      String groupLeadInsertSql = "insert into " + _accountsSchema + "SUBSCRIPTION_GROUP_LEADS (GROUP_ID, USER_ID) values (?, ?)";
      Reducer<Group, List<Object[]>> leadReducer = (l,g) -> {
        l.addAll(g.leadUserIds.stream().map(id -> new Object[]{ g.groupId, id }).collect(Collectors.toList())); return l; };
      List<Object[]> leadMapping = Functions.reduce(groups.values(), leadReducer, new ArrayList<>());
      LOG.info("Inserting " + leadMapping.size() + " rows into subscription_group_leads table");
      tableCounts.getJSONObject("subscription_group_leads").put("after", leadMapping.size());
      new SQLRunner(_acctDbDs, groupLeadInsertSql).executeStatementBatch(new BasicArgumentBatch() {
        @Override public Iterator<Object[]> iterator() { return leadMapping.iterator(); }
      }
      .setBatchSize(leadMapping.size())
      .setParameterTypes(new Integer[] { Types.BIGINT, Types.BIGINT }));

      // insert group membership
      String subscriptionTokenInsertSql = "insert into " + _accountsSchema + "account_properties (user_id, key, value) values (?, '" + SUBSCRIPTION_TOKEN_KEY + "', ?)";
      Reducer<Group, List<Object[]>> memberReducer = (l,g) -> {
        l.addAll(g.memberUserIds.stream().map(id -> new Object[]{ id, g.subscriptionToken }).collect(Collectors.toList())); return l; };
      List<Object[]> memberMapping = Functions.reduce(groups.values(), memberReducer, new ArrayList<>());
      LOG.info("Inserting " + memberMapping.size() + " rows into account_properties table");
      tableCounts.getJSONObject("account_properties").put("after", memberMapping.size());
      new SQLRunner(_acctDbDs, subscriptionTokenInsertSql).executeStatementBatch(new BasicArgumentBatch() {
        @Override public Iterator<Object[]> iterator() { return memberMapping.iterator(); }
      }
      .setBatchSize(memberMapping.size())
      .setParameterTypes(new Integer[] { Types.BIGINT, Types.VARCHAR }));
    }

    return result;
  }
}
