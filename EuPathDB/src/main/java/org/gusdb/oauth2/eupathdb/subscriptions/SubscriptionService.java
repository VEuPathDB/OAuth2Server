package org.gusdb.oauth2.eupathdb.subscriptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;
import org.gusdb.oauth2.server.OAuthServlet;
import org.gusdb.oauth2.service.Session;
import org.json.JSONArray;

@Path("/")
public class SubscriptionService {

  private static final Logger LOG = LogManager.getLogger(SubscriptionService.class);

  private static final String TSV_MEDIA_TYPE = "text/tab-separated-values";

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

  @GET
  @Path("users")
  @Produces(TSV_MEDIA_TYPE + ";charset=utf-8")
  public Response getAccountsDetails() {

    assertAdmin();

    StreamingOutput writer = out -> {
      AccountDbAuthenticator authenticator = getAuthenticator();
      new BulkDataDumper(
          authenticator.getAccountDb(),
          authenticator.getUserAccountsSchema()
      ).writeAccountDetails(out);
    };

    String contentDisposition = "attachment; filename=accountdb-dump." +
    new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".tsv";

    return Response
        .ok(writer)
        .header("Content-Disposition", contentDisposition)
        .build();
  }

  @GET
  @Path("groups")
  @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
  public Response getSubscribedGroups() {

    AccountDbAuthenticator authenticator = getAuthenticator();
    JSONArray groupsJson = new BulkDataDumper(
        authenticator.getAccountDb(),
        authenticator.getUserAccountsSchema()
    ).getGroupsJson();

    return Response.ok(groupsJson.toString(2)).build();
  }

  
  
  /* What does Nupur have to do?
   * 
   * Main Menu / Back to Main Menu
   * What do you want to do?
   * - Add a subscriber -> empty form, redirect to View a Subscriber
   * - View/Edit a subscriber -> show subscriber name w/group links "Click to View/Edit a Group", edit button
   * -     edit button goes to form with values filled in and different action link, redirect to View
   * - Add a group -> empty form, redirect to View a Group
   * - View/Edit a group -> similar flow
   * 
   * Endpoints:
   * GET  /subscribers (for select box)
   * POST /subscribers
   * GET  /subscribers/{id}
   * POST /subscribers/{id}
   * POST /groups
   * GET  /groups/{id}
   * POST /groups/{id}
   * GET  /user-names?userId1,userId2 (just for the check!)
   * 
   * Add new subscriber:
- Name
- IsActive (default to checked)

Add new group:
- Subscriber (select from subscribers)
- Name (group_clean) filled in with subscriber name if empty
- (optional) GroupLeadUserIDs (comma-delimited) maybe with check button to show name?

Update subscription
- Name
- IsActive

Update existing group:
- Name
- GroupLeadUserIDs (comma-delimited) maybe with check button to show name?
   */
  
}
