package org.gusdb.oauth2.eupathdb.subscriptions;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonValue;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.BadRequestException;
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

/**
 * Provides the following endpoints to support the subscription management:
 *
 * GET  /admin                       redirects to admin homepage
 * GET  /subscribers                 data to fill subscribers select box
 * POST /subscribers                 create a new subscriber
 * GET  /subscribers/{id}            get an existing subscriber
 * POST /subscribers/{id}            edit an existing subscriber
 * POST /groups                      create a new group
 * GET  /groups/{id}                 get an existing group
 * POST /groups/{id}                 edit an existing group
 * POST /groups/{id}/add-members     adds users to an existing group (as members, not leads)
 * GET  /user-names?userId1,userId2  returns username details to allow admins to check IDs
 *
 * Mutable subscriber fields:
 * - Name
 * - IsActive (default to checked)
 *
 * Mutable group fields:
 * - Subscriber (select from subscribers)
 * - Name (group_clean) filled in with subscriber name if empty
 * - (optional) GroupLeadUserIDs (comma-delimited) maybe with check button to show name?
 */
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
  @Path("admin")
  public Response adminRedirect() throws URISyntaxException {
    return Response.seeOther(new URI("assets/admin/home.html")).build();
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

  @GET
  @Path("subscriptions")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscriptions() {
    assertAdmin();
    return Response
      .ok(JsonCache.getSubscriptionJson(() -> new JSONArray(
        getSubscriptionManager()
          .getSubscriptions()
          .stream()
          .map(Subscription::toJson)
          .collect(Collectors.toList())
      ).toString()))
      .build();
  }

  @POST
  @Path("subscriptions")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addSubscription(String body) {
    assertAdmin();
    try {
      Subscription sub = new Subscription(getAccountDb(), new JSONObject(body));
      getSubscriptionManager().addSubscription(sub);
      JsonCache.expireSubscriptionsJson();
      return Response.ok(sub.toJson().toString()).build();
    }
    catch (RuntimeException e) {
      throw translateRuntimeException(e);
    }
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
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateSubscription(@PathParam("id") String subscriptionId, String body) {
    assertAdmin();
    try {
      // make sure this is a legit subscription ID
      getSubscriptionManager().getSubscription(Long.valueOf(subscriptionId));

      // update subscription
      getSubscriptionManager().updateSubscription(
          new Subscription(subscriptionId, new JSONObject(body)));

      JsonCache.expireSubscriptionsJson();
      return Response.noContent().build();
    }
    catch (RuntimeException e) {
      throw translateRuntimeException(e);
    }
  }

  @GET
  @Path("groups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getGroups(@QueryParam("includeUnsubscribedGroups") @DefaultValue("false") boolean includeUnsubscribedGroups) {
    LOG.info("Getting groups, includeUnsubscribedGroups = " + includeUnsubscribedGroups);
    return Response.ok(JsonCache.getGroupsJson(() ->
      new BulkDataDumper(getAccountDb())
        .getGroupsJson(includeUnsubscribedGroups)
        .toString()
    )).build();
  }

  @POST
  @Path("groups")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response addGroup(String body) {
    assertAdmin();
    try {
      Group group = new Group(getAccountDb(), new JSONObject(body));
      getSubscriptionManager().addGroup(group,
          SubscriptionTokenGenerator.getNewToken());
      LOG.info("Creating new group. makeLeadsMembers = " + group.makeLeadsMembers());
      if (group.makeLeadsMembers()) {
        getSubscriptionManager().assignUsersToGroup(group.getGroupId(), group.getGroupLeadIds());
      }
      JsonCache.expireGroupsJson();
      return Response.ok(group.toJson().toString()).build();
    }
    catch (RuntimeException e) {
      throw translateRuntimeException(e);
    }
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
  @Consumes(MediaType.APPLICATION_JSON)
  public Response updateGroup(@PathParam("id") String groupId, String body) {
    assertAdmin();
    try {
      // make sure this is a legit group ID
      getSubscriptionManager().getGroup(Long.valueOf(groupId));

      // update group
      Group group = new Group(groupId, new JSONObject(body));
      getSubscriptionManager().updateGroup(group);
      if (group.makeLeadsMembers()) {
        getSubscriptionManager().assignUsersToGroup(group.getGroupId(), group.getGroupLeadIds());
      }

      JsonCache.expireGroupsJson();
      return Response.noContent().build();
    }
    catch (RuntimeException e) {
      throw translateRuntimeException(e);
    }
  }

  @POST
  @Path("groups/{id}/add-members")
  @Consumes(MediaType.APPLICATION_JSON)
  public Response assignUsersToGroup(@PathParam("id") String groupIdStr, String body) {
    assertAdmin();
    try {
      // make sure this is a legit group ID
      long groupId = Long.valueOf(groupIdStr);
      getSubscriptionManager().getGroup(groupId);

      // parse user IDs from request body
      JSONArray userIdsJson = new JSONArray(body);
      List<Long> userIds = new ArrayList<>();
      for (int i = 0; i < userIdsJson.length(); i++) {
        userIds.add(userIdsJson.getLong(i));
      }

      // assign all passed users to this group
      getSubscriptionManager().assignUsersToGroup(groupId, userIds);

      return Response.noContent().build();
    }
    catch (RuntimeException e) {
      throw translateRuntimeException(e);
    }
  }

  @GET
  @Path("user-names")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserNames(@QueryParam("userIds") String userIdsStr) {
    assertAdmin();
    List<Long> userIds = Arrays.asList(userIdsStr.split(",")).stream()
        .map(s -> Long.parseLong(s.trim())).collect(Collectors.toList());
    JsonValue queryResult = getAuthenticator().executeQuery(
        Json.createObjectBuilder().add("userIds",
            Json.createArrayBuilder(userIds)).build());
    return Response.ok(queryResult.toString()).build();
  }

  private RuntimeException translateRuntimeException(RuntimeException e) {
    if (e.getCause() != null && e.getCause().getMessage().contains("ORA-00001: unique constraint")) {
      throw new BadRequestException("Name already in use");
    }
    throw e;
  }
}
