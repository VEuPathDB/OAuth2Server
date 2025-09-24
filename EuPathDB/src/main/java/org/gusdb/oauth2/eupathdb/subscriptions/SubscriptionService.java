package org.gusdb.oauth2.eupathdb.subscriptions;

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
  @Produces(TSV_MEDIA_TYPE)
  public Response getAccountsDetails() {

    assertAdmin();

    StreamingOutput writer = out -> {
      AccountDbAuthenticator authenticator = getAuthenticator();
      new BulkDataDumper(
          authenticator.getAccountDb(),
          authenticator.getUserAccountsSchema()
      ).writeAccountDetails(out);
    };

    return Response.ok(writer).build();
  }

  @GET
  @Path("groups")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getSubscribedGroups() {

    AccountDbAuthenticator authenticator = getAuthenticator();
    JSONArray groupsJson = new BulkDataDumper(
        authenticator.getAccountDb(),
        authenticator.getUserAccountsSchema()
    ).getGroupsJson();

    return Response.ok(groupsJson.toString(2)).build();
  }
}
