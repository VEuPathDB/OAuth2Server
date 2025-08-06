package org.gusdb.oauth2.eupathdb.subscriptions;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.gusdb.oauth2.config.ApplicationConfig;
import org.gusdb.oauth2.eupathdb.AccountDbAuthenticator;
import org.gusdb.oauth2.server.OAuthServlet;

public class SubscriptionService {

  @Context
  private ServletContext _context;

  @Context
  private HttpServletRequest _request;

  @Context
  private HttpHeaders _headers;

  private ApplicationConfig getApplicationConfig() {
    return OAuthServlet.getApplicationConfig(_context);
  }

  private AccountDbAuthenticator getAuthenticator() {
    return (AccountDbAuthenticator) OAuthServlet.getAuthenticator(_context);
  }

  // returns list of subscriptions
  @GET
  @Path("/subscriptions")
  public Response getSubscriptions() {
    return Response.ok().build();
  }

  // returns info about a single subscription including groups/leads/members
  @GET
  @Path("/subscriptions/{subscriptionId}")
  public Response getSubscription(@QueryParam("subscriptionId") int subscriptionId) {
    return Response.ok().build();
  }

  // returns known groups that are not subscribed
  @GET
  @Path("/groups/orphan")
  public Response getOrphanGroups() {
    return Response.ok().build();
  }

}
