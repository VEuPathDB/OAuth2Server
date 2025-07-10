package org.gusdb.oauth2.eupathdb.subscriptions;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/subscriptions")
public class SubscriptionService {

  // insert subscriber id and expiration date
  @POST
  public Response createSubscription(String body) {
    
  }

  // allow update to subscriber id or expiration date (by email?)
  @POST
  @Path("{subscriptionId}")
  public Response updateSubscription(@QueryParam("subscriptionId") int subscriptionId, String body) {
    
  }

  @POST
  @Path("{subscriptionId}/groups")

  
}
