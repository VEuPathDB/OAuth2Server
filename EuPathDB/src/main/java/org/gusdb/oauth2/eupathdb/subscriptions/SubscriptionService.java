package org.gusdb.oauth2.eupathdb.subscriptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.sql.DataSource;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;
import org.gusdb.oauth2.server.OAuthServlet;
import org.json.JSONArray;
import org.json.JSONObject;

public class SubscriptionService {

  @Context
  private ServletContext _context;

  private DataSource getAcctDbDs() {
    return ((AccountDbAuthenticator) OAuthServlet.getAuthenticator(_context))
        .getAccountDb().getDataSource();
  }

  private static final String GROUP_LEADS_SQL_PATH = "sql/select-group-leads.sql";
  private static final String GROUP_LEADS_SQL = getGroupLeadsSql();

  // returns list of subscribed groups
  @GET
  @Path("/groups")
  public Response getSubscribedGroups() {
    // TODO: break DB work out of the service class; ok here for now
    return Response.ok(new SQLRunner(getAcctDbDs(), GROUP_LEADS_SQL).executeQuery(rs -> {
      Map<String, JSONObject> groups = new HashMap<>(); // keyed on subscription token

      // each row represents a group + group lead (group lead may be null if group has no leads)
      while (rs.next()) {

        // parse columns
        String subscriptionToken = rs.getString("subscription_token");
        String groupName = rs.getString("group_name");
        String leadFirstName = rs.getString("first_name");
        String leadLastName = rs.getString("last_name");
        String leadOrganization = rs.getString("organization");

        // see if group already exists and create new one if not
        JSONObject group = groups.get(subscriptionToken);
        if (group == null) {
          group = new JSONObject()
              .put("subscriptionToken", subscriptionToken)
              .put("groupName", groupName)
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
    }).toString(2)).build();
  }

  private static String getGroupLeadsSql() {
    URL url = Thread.currentThread().getContextClassLoader().getResource(GROUP_LEADS_SQL_PATH);
    if (url == null) throw new RuntimeException("Unable to read resource " + GROUP_LEADS_SQL_PATH);
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
      return IoUtil.readAllChars(reader);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to read resource " + GROUP_LEADS_SQL_PATH, e);
    }
  }

}
