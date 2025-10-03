package org.gusdb.oauth2.eupathdb.subscriptions;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonValue;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;
import org.gusdb.oauth2.eupathdb.AccountDbInfo;
import org.gusdb.oauth2.eupathdb.tools.SubscriptionTokenGenerator;
import org.gusdb.oauth2.server.OAuthServlet;
import org.gusdb.oauth2.service.Session;
import org.json.JSONArray;
import org.json.JSONObject;

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

  private AccountDbInfo getAccountDb() {
    return getAuthenticator().getAccountDbInfo();
  }

  private SubscriptionManager getSubscriptionManager() {
    return new SubscriptionManager(getAccountDb());
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
  @Produces(TSV_MEDIA_TYPE)
  public Response getAccountsDetails() {

    assertAdmin();

    StreamingOutput writer = out -> {
      new BulkDataDumper(getAccountDb()).writeAccountDetails(out);
    };

    String contentDisposition = "attachment; filename=accountdb-dump." +
    new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".tsv";

    return Response
        .ok(writer)
        .header("Content-Disposition", contentDisposition)
        .build();
  }

  /**
   * @return [{ id, name, isActive }]
   *    round 2: list of groups in the subscription
   */
  @GET
  @Path("subscriptions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptions() {
    assertAdmin();
    return Response
      .ok(new JSONArray(
        getSubscriptionManager()
          .getSubscriptions()
          .stream()
          .map(Subscription::toJson)
          .collect(Collectors.toList())
      ).toString())
      .build();
  }

  @POST
  @Path("subscriptions")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response addSubscription(String body) {
    assertAdmin();
    getSubscriptionManager().addSubscription(
        new Subscription(getAccountDb(), new JSONObject(body)));
    return Response.noContent().build();
  }

  @GET
  @Path("subscriptions/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriber(@PathParam("id") String subscriberId) {
    assertAdmin();
    return Response
      .ok(getSubscriptionManager()
        .getSubscription(Long.parseLong(subscriberId))
        .toJson()
        .toString())
      .build();
  }

  @POST
  @Path("subscriptions/{id}")
  public Response updateSubscription(@PathParam("id") String subscriptionId, String body) {
    assertAdmin();
    getSubscriptionManager().updateSubscription(
        new Subscription(subscriptionId, new JSONObject(body)));
    return Response.noContent().build();
  }


  @GET
  @Path("groups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGroups(@QueryParam("includeUnsubscribedGroups") @DefaultValue("false") boolean includeUnsubscribedGroups) {
    JSONArray groupsJson = new BulkDataDumper(getAccountDb())
        .getGroupsJson(includeUnsubscribedGroups);
    return Response.ok(groupsJson.toString()).build();
  }

  @POST
  @Path("groups")
  public Response addGroup(String body) {
    assertAdmin();
    getSubscriptionManager().addGroup(
        new Group(getAccountDb(), new JSONObject(body)),
        SubscriptionTokenGenerator.getNewToken());
    return Response.noContent().build();
  }

  @GET
  @Path("groups/{id}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGroup(@PathParam("id") String groupId) {
    assertAdmin();
    return Response
        .ok(getSubscriptionManager()
          .getGroup(Long.parseLong(groupId))
          .toJson()
          .toString())
        .build();
  }

  @POST
  @Path("groups/{id}")
  public Response updateGroup(@PathParam("id") String groupId, String body) {
    assertAdmin();
    getSubscriptionManager().updateGroup(
        new Group(groupId, new JSONObject(body)));
    return Response.noContent().build();
  }

  @GET
  @Path("user-names/{userIds}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserNames(@PathParam("userIds") String userIdsStr) {
    assertAdmin();
    List<Long> userIds = Arrays.asList(userIdsStr.split(",")).stream().map(s -> Long.parseLong(s)).collect(Collectors.toList());
    JsonValue queryResult = getAuthenticator().executeQuery(
        Json.createObjectBuilder().add("userIds",
            Json.createArrayBuilder(userIds)).build());
    return Response.ok(queryResult.toString()).build();
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
