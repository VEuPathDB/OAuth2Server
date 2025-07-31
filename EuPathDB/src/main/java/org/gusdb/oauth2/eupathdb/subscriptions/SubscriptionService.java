package org.gusdb.oauth2.eupathdb.subscriptions;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

public class SubscriptionService {

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
