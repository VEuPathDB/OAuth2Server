package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.List;

import org.json.JSONObject;

public class Group {

  public static class SimpleUser {
    long userId;
    String name;
    String organization;
  }

  long groupId;
  long subscriberId;
  String displayName;
  String subscriptionToken;
  List<SimpleUser> leads;
  List<SimpleUser> members;

  public Group(JSONObject jsonObject) {
    // TODO Auto-generated constructor stub
  }

  public Group(String groupId, JSONObject jsonObject) {
    // TODO Auto-generated constructor stub
  }

  public JSONObject toJson() {
    // TODO Auto-generated method stub
    return null;
  }

}
