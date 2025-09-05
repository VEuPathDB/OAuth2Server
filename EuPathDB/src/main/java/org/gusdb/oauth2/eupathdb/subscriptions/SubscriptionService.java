package org.gusdb.oauth2.eupathdb.subscriptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import javax.ws.rs.Consumes;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.gusdb.fgputil.IoUtil;
import org.gusdb.fgputil.db.pool.DatabaseInstance;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;
import org.gusdb.oauth2.server.OAuthServlet;
import org.gusdb.oauth2.service.Session;
import org.json.JSONArray;
import org.json.JSONObject;

@Path("/groups")
public class SubscriptionService {

  private static final Logger LOG = LogManager.getLogger(SubscriptionService.class);

  @Context
  private ServletContext _context;

  @Context
  private HttpServletRequest _request;

  private AccountDbAuthenticator getAuthenticator() {
    return ((AccountDbAuthenticator) OAuthServlet.getAuthenticator(_context));
  }

  private void assertAdmin() {
    Session session = new Session(_request.getSession());
    String userId = "none";
    List<String> adminUserIds = ((AccountDbAuthenticator) OAuthServlet.getAuthenticator(_context)).getAdminUserIds();
    if (session.isAuthenticated()) {
      // user is logged in; get user ID and compare to known admin IDs
      userId = session.getUserId();
      if (adminUserIds.contains(userId)) {
        // current user is an admin
        return;
      }
    }
    LOG.warn("Attempt by " + userId + " to access admin endpoint denied (must be one of [ " + String.join(", ", adminUserIds) + " ].");
    throw new ForbiddenException();
  }

  private static final String GROUP_LEADS_SQL_PATH = "sql/select-group-leads.sql";
  private static final String GROUP_LEADS_SQL = getGroupLeadsSql();

  // returns list of subscribed groups
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscribedGroups() {

    AccountDbAuthenticator authenticator = getAuthenticator();
    DataSource ds = authenticator.getAccountDb().getDataSource();
    String sql = GROUP_LEADS_SQL.replace("$$accountschema$$", authenticator.getUserAccountsSchema());
    return Response.ok(new SQLRunner(ds, sql).executeQuery(rs -> {
      Map<String, JSONObject> groups = new LinkedHashMap<>(); // keyed on subscription token

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

  @POST
  @Path("upload")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Produces(MediaType.APPLICATION_JSON)
  public Response uploadNewGroupsFile(
      @FormDataParam("file") InputStream uploadedInputStream,
      @FormDataParam("file") FormDataContentDisposition fileDetail,
      @FormDataParam("writeToDb") String writeToDbStr,
      @FormDataParam("returnGroupDetail") String returnGroupDetailStr) throws IOException {

    assertAdmin();

    LOG.info("Handling upload request. file=" + fileDetail.getFileName() +
        ", writeToDb=" + writeToDbStr + ", returnGroupDetail=" + returnGroupDetailStr);
    boolean writeToDb = "on".equals(writeToDbStr);
    boolean returnGroupDetail = "on".equals(returnGroupDetailStr);

    // save uploaded file into temporary location
    String uploadedFileLocation = "/tmp/" + fileDetail.getFileName() + "_" + UUID.randomUUID().toString();
    try (OutputStream out = new FileOutputStream(new File(uploadedFileLocation))) {
      uploadedInputStream.transferTo(out);
    }
    catch (IOException e) {
      LOG.error("Could not store uploaded file", e);
      throw e;
    }

    AccountDbAuthenticator authenticator = getAuthenticator();
    DatabaseInstance acctDb = authenticator.getAccountDb();
    JSONObject result = new SubscriptionGroupReloader(acctDb.getDataSource(), acctDb.getPlatform(), authenticator.getUserAccountsSchema())
      .loadSubscriptions(Paths.get(uploadedFileLocation), returnGroupDetail, writeToDb);

    return Response.ok(result.toString(2)).build();
  }

}
