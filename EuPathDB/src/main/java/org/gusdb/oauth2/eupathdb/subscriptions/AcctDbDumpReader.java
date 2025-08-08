package org.gusdb.oauth2.eupathdb.subscriptions;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintStream;
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
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.BasicArgumentBatch;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.fgputil.functional.FunctionalInterfaces.Reducer;
import org.gusdb.fgputil.functional.Functions;
import org.gusdb.fgputil.iterator.IteratorUtil;
import org.json.JSONArray;

public class AcctDbDumpReader {

  private static final SupportedPlatform PLATFORM = SupportedPlatform.ORACLE;

  private static final boolean PRINT_WARNINGS = true;
  private static final boolean PRINT_PARSED_GROUPS = false;
  private static final boolean PRINT_SORTED_NAMES = false;
  private static final boolean WRITE_TO_DB = false;

  private static final String ACCOUNT_SCHEMA = "useraccounts.";

  private static final String SUBSCRIPTION_TOKEN_KEY = "subscription_token";

  public static void log(String s) {
    System.out.println("INFO: " + s);
  }

  public static void warn(String s) {
    if (PRINT_WARNINGS) System.err.println("WARNING: " + s);
  }

  public static void main(String[] args) throws Exception {
    if (args.length != 5) {
      System.err.println("USAGE: AcctDbDumpReader <invoice_file> <annotated_accounts_file> <db_connection_string> <db_user> <db_password>");
      System.exit(1);
    }
    String invoiceFile = args[0];
    String accountsFile = args[1];
    String dbConnectionString = args[2];
    String dbUser = args[3];
    String dbPass = args[4];
    SimpleDbConfig dbConfig = SimpleDbConfig.create(PLATFORM, dbConnectionString, dbUser, dbPass, 2);
    new AcctDbDumpReader(Paths.get(invoiceFile), Paths.get(accountsFile), dbConfig).loadSubscriptions();
  }

  @SuppressWarnings("unused")
  private final Path _invoiceFile;
  private final Path _accountsFile;
  private final SimpleDbConfig _dbConfig;
  
  public AcctDbDumpReader(Path invoiceFile, Path accountsFile, SimpleDbConfig dbConfig) {
    _invoiceFile = invoiceFile;
    _accountsFile = accountsFile;
    _dbConfig = dbConfig;
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

    public void print(PrintStream out) {
      out.println("======================================");
      out.println("groupId:           " + groupId);
      out.println("subscriptionToken: " + subscriptionToken);
      out.println("name:              " + name);
      out.println("hasInvoice:        " + hasInvoice);
      out.println("numMembers:        " + memberUserIds.size());
      out.println("memberUserIds:     " + new JSONArray(memberUserIds).toString());
      out.println("leadUserIds:       " + new JSONArray(leadUserIds).toString());
    }
  }

  // for use with randomAlphaNumericString
  private static final String ALPHA = "abcdefghijklmnopqrstuvwxyz";
  static final String RANDOM_CHARS = "0123456789" + ALPHA + ALPHA.toUpperCase();

  // copied from WDK's Utilities.java
  public static String randomAlphaNumericString(int numChars) {
    StringBuilder str = new StringBuilder();
    new Random()
      .ints(numChars, 0, RANDOM_CHARS.length())
      .forEach(i -> str.append(RANDOM_CHARS.charAt(i)));
    return str.toString();
  }

  private void loadSubscriptions() throws Exception {

    log("Opening connection to database");
    try (DatabaseInstance dbInstance = new DatabaseInstance(_dbConfig)) {

      DataSource ds = dbInstance.getDataSource();

      // data structure for the groups (key = group_clean aka name)
      Map<String, Group> groups = new LinkedHashMap<>();
  
      // first load accounts file
      log("Opening accounts file");
      try (BufferedReader in = new BufferedReader(new FileReader(_accountsFile.toFile()))) {
        in.readLine(); // skip header
        while (in.ready()) {
  
          // read data in the row, skipping rows without enough columns
          String[] tokens = in.readLine().split("\t");
          if (tokens.length < 16) {
            warn(tokens.length == 0 ? "Skipping empty line" : "Line for user " + tokens[0] + " does not have enough columns.");
            continue;
          }
  
          // get the columns we care about
          Long userId = Long.parseLong(tokens[0].trim());
          String groupClean = tokens[12].trim();
          boolean hasInvoice = tokens[14].trim().equals("Yes");
          boolean isLead = tokens[15].trim().equals("Yes");
  
          if (groupClean.isEmpty()) {
            if (hasInvoice || isLead) {
              warn("User " + userId + " does not have a group_clean but has Yes in the invoice (" + hasInvoice + ") or lead (" + isLead + ") column.");
            }
            continue; // nothing to update
          }
  
          // try to find existing group
          Set<Long> inconsistentGroups = new HashSet<>();
          Group group = groups.get(groupClean);
          if (group != null) {
            // perform checks
            if (group.hasInvoice != hasInvoice && !inconsistentGroups.contains(group.groupId)) {
              warn("Group '" + groupClean + "' has inconsistent hasInvoice information.");
              // if any group has hasInvoice=true, set to true
              group.hasInvoice = true;
            }
          }
          else {
            // make a new group
            group = new Group(ds);
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
          warn("Group '" + group.name + "' does not have any leads.");
        }
      }
  
      if (PRINT_PARSED_GROUPS) {
        for (Group group : groups.values()) {
          group.print(System.out);
        }
      }

      if (PRINT_SORTED_NAMES) {
        String[] sortedNames = new ArrayList<>(groups.keySet()).toArray(new String[0]);
        Arrays.sort(sortedNames);
        for (String name : sortedNames) {
          System.out.println(name);
        }
      }

      if (WRITE_TO_DB) {

        // insert subscription records (one for each group for now since we are only reading the accounts file)
        String subscriptionInsertSql = "insert into " + ACCOUNT_SCHEMA + "subscriptions (subscription_id, is_active) values (?, ?)";
        Iterator<Object[]> subIter = IteratorUtil.transform(groups.values().iterator(),
            group -> new Object[] { group.subscriptionId, group.hasInvoice });
        log("Inserting " + groups.size() + " rows into subscriptions table");
        new SQLRunner(ds, subscriptionInsertSql).executeStatementBatch(new BasicArgumentBatch() {
          @Override public Iterator<Object[]> iterator() { return subIter; }
        }
        .setBatchSize(groups.size())
        .setParameterTypes(new Integer[] { Types.BIGINT, PLATFORM.getPlatformInstance().getBooleanType() }));
  
        // insert group records
        String groupInsertSql = "insert into " + ACCOUNT_SCHEMA + "subscription_groups (group_id, subscription_id, group_name, subscription_token) values (?, ?, ?, ?)";
        Iterator<Object[]> groupIter = IteratorUtil.transform(groups.values().iterator(),
            group -> new Object[] { group.groupId, group.subscriptionId, group.name, group.subscriptionToken });
        log("Inserting " + groups.size() + " rows into subscription_groups table");
        new SQLRunner(ds, groupInsertSql).executeStatementBatch(new BasicArgumentBatch() {
          @Override public Iterator<Object[]> iterator() { return groupIter; }
        }
        .setBatchSize(groups.size())
        .setParameterTypes(new Integer[] { Types.BIGINT, Types.BIGINT, Types.VARCHAR, Types.VARCHAR }));
  
        // insert group leads
        String groupLeadInsertSql = "insert into " + ACCOUNT_SCHEMA + "SUBSCRIPTION_GROUP_LEADS (GROUP_ID, USER_ID) values (?, ?)";
        Reducer<Group, List<Object[]>> leadReducer = (l,g) -> {
          l.addAll(g.leadUserIds.stream().map(id -> new Object[]{ g.groupId, id }).collect(Collectors.toList())); return l; };
        List<Object[]> leadMapping = Functions.reduce(groups.values(), leadReducer, new ArrayList<>());
        log("Inserting " + leadMapping.size() + " rows into subscription_group_leads table");
        new SQLRunner(ds, groupLeadInsertSql).executeStatementBatch(new BasicArgumentBatch() {
          @Override public Iterator<Object[]> iterator() { return leadMapping.iterator(); }
        }
        .setBatchSize(leadMapping.size())
        .setParameterTypes(new Integer[] { Types.BIGINT, Types.BIGINT }));
  
        // insert group membership
        String subscriptionTokenInsertSql = "insert into " + ACCOUNT_SCHEMA + "account_properties (user_id, key, value) values (?, '" + SUBSCRIPTION_TOKEN_KEY + "', ?)";
        Reducer<Group, List<Object[]>> memberReducer = (l,g) -> {
          l.addAll(g.memberUserIds.stream().map(id -> new Object[]{ id, g.subscriptionToken }).collect(Collectors.toList())); return l; };
        List<Object[]> memberMapping = Functions.reduce(groups.values(), memberReducer, new ArrayList<>());
        log("Inserting " + memberMapping.size() + " rows into account_properties table");
        new SQLRunner(ds, subscriptionTokenInsertSql).executeStatementBatch(new BasicArgumentBatch() {
          @Override public Iterator<Object[]> iterator() { return memberMapping.iterator(); }
        }
        .setBatchSize(memberMapping.size())
        .setParameterTypes(new Integer[] { Types.BIGINT, Types.VARCHAR }));
      }

      log("Closing connection to DB");
    }
    log ("Done");
  }
}
