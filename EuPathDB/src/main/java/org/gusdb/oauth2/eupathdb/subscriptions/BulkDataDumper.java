package org.gusdb.oauth2.eupathdb.subscriptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.db.platform.SupportedPlatform;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.pool.SimpleDbConfig;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.oauth2.eupathdb.AccountDbInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class BulkDataDumper {

  // edit these values to run locally
  private static final String ACCTDB_CONNECTION_URL = "jdbc:oracle:thin:@//localhost:5011/acctdb.upenn.edu";
  private static final String ACCOUNTS_SCHEMA = "useraccounts.";
  private static final String ACCTDB_USER = "*****";
  private static final String ACCTDB_PASS = "*****";
  private static final String OUTPUT_FILE = "/home/myuser/Desktop/accountdb-dump." + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".tsv";
  
  public static void main(String[] args) throws Exception {
    System.out.println("Connecting to database...");
    try (FileOutputStream output = new FileOutputStream(OUTPUT_FILE);
         DatabaseInstance db = new DatabaseInstance(SimpleDbConfig.create(
            SupportedPlatform.ORACLE, ACCTDB_CONNECTION_URL, ACCTDB_USER, ACCTDB_PASS))) {
      System.out.println("Writing accounts dump...");
      new BulkDataDumper(new AccountDbInfo(db, ACCOUNTS_SCHEMA)).writeAccountDetails(output);
      System.out.println("Done writing file.");
    }
    System.out.println("Database connection closed.");
  }

  private static final String ACCOUNTS_SCHEMA_MACRO = "$$accountschema$$";
  private static final String ALLOWED_IS_ACTIVE_VALUES_MACRO = "$$allowedIsActiveValues$$";

  private static final String GROUP_LEADS_SQL = readResourceSql("sql/select-group-vocabulary.sql");
  private static final String ACCOUNTS_DETAILS_SQL = readResourceSql("sql/select-accounts-details.sql");

  private final AccountDbInfo _db;

  public BulkDataDumper(AccountDbInfo db) {
    _db = db;
  }

  public void writeAccountDetails(OutputStream outStream) {

    String sql = ACCOUNTS_DETAILS_SQL.replace(ACCOUNTS_SCHEMA_MACRO, _db.SCHEMA);
    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outStream, StandardCharsets.UTF_8));
    List<String> buffer = new ArrayList<>();

    new SQLRunner(_db.DATASOURCE, sql).executeQuery(rs -> {
      try {
        int numCols = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= numCols; i++) {
          buffer.add(rs.getMetaData().getColumnName(i));
        }
        out.write(String.join("\t", buffer));
        out.newLine();
        buffer.clear();
        while (rs.next()) {
          for (int i = 1; i <= numCols; i++) {
            buffer.add(String.valueOf(rs.getObject(i)));
          }
          out.write(String.join("\t", buffer));
          out.newLine();
          buffer.clear();
        }
        out.flush();
        return null;
      }
      catch (IOException e) {
        throw new RuntimeException("Could not write to output file", e);
      }
    });
  }

  public JSONArray getGroupsJson(boolean includeUnsubscribedGroups) {

    String isActiveValues = includeUnsubscribedGroups ? "1, 0" : "1";

    String sql = GROUP_LEADS_SQL
        .replace(ACCOUNTS_SCHEMA_MACRO, _db.SCHEMA)
        .replace(ALLOWED_IS_ACTIVE_VALUES_MACRO, isActiveValues);

    return new SQLRunner(_db.DATASOURCE, sql).executeQuery(rs -> {

      Map<String, JSONObject> groups = new LinkedHashMap<>(); // keyed on subscription token

      // each row represents a group + group lead (group lead may be null if group has no leads)
      while (rs.next()) {

        // parse columns
        long groupId = rs.getLong("group_id");
        long subscriptionId = rs.getLong("subscription_id");
        String subscriptionToken = rs.getString("subscription_token");
        String groupName = rs.getString("group_name");
        String leadFirstName = rs.getString("first_name");
        String leadLastName = rs.getString("last_name");
        String leadOrganization = rs.getString("organization");
        String subscriberName = rs.getString("subscriber_name");

        // only include subscriberName if it differs from groupName
        if (subscriberName.equals(groupName)) {
          subscriberName = null; // null value will be omitted by org.json
        }

        // see if group already exists and create new one if not
        JSONObject group = groups.get(subscriptionToken);
        if (group == null) {
          group = new JSONObject()
              .put("groupId", groupId)
              .put("subscriptionId", subscriptionId)
              .put("subscriptionToken", subscriptionToken)
              .put("groupName", groupName)
              .put("subscriberName", subscriberName)
              .put("groupLeads", new JSONArray());
          groups.put(subscriptionToken, group);
        }

        // add lead to this group if lead is present
        if (leadFirstName != null) {
          group.getJSONArray("groupLeads").put(new JSONObject()
              .put("name",leadFirstName + " " + leadLastName)
              .put("organization", leadOrganization));
        }
      }

      return new JSONArray(groups.values());
    });
  }

  public static String readResourceSql(String resourceName) {
    URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
    if (url == null) throw new RuntimeException("Unable to read resource " + resourceName);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      return IoUtil.readAllChars(reader);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to read resource " + resourceName, e);
    }
  }
}
