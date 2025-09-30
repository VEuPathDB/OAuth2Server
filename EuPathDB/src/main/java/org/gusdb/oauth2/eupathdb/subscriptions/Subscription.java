package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.List;

import org.json.JSONObject;

public class Subscription {

  long subscriberId;
  boolean isActive;
  String displayName;
  List<Group> groups;

  public Subscription(JSONObject subscriber) {
    // TODO Auto-generated constructor stub
  }

  public Subscription(String subscriptionId, JSONObject jsonObject) {
    // TODO Auto-generated constructor stub
  }

  public JSONObject toJson() {
    // TODO Auto-generated constructor stub
    return null;
  }
}
