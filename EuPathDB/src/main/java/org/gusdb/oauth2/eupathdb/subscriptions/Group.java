package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.gusdb.oauth2.eupathdb.AccountDbInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Group {

  public static class SimpleUser {

    private long _userId;
    private String _name;
    private String _organization;

    public SimpleUser(long userId, String name, String organization) {
      _userId = userId;
      _name = name;
      _organization = organization;
    }

    public JSONObject toJson() {
      return new JSONObject()
          .put("userId", _userId)
          .put("name", _name)
          .put("organization", _organization);
    }
  }

  public static class GroupWithUsers {

    private Group _group;
    private String _subscriptionToken;
    private List<SimpleUser> _leads;
    private List<SimpleUser> _members;

    public GroupWithUsers(Group group, String subscriptionToken, List<SimpleUser> leads, List<SimpleUser> members) {
      _group = group;
      _subscriptionToken = subscriptionToken;
      _leads = leads;
      _members = members;
    }

    public JSONObject toJson() {
      return _group.toJson()
          .put("subscriptionToken", _subscriptionToken)
          .put("leadUsers", new JSONArray(_leads.stream().map(SimpleUser::toJson).collect(Collectors.toList())))
          .put("members", new JSONArray(_members.stream().map(SimpleUser::toJson).collect(Collectors.toList())));
    }
  }

  private long _groupId;
  private long _subscriptionId;
  private String _displayName;
  private List<Long> _groupLeadIds;
  private boolean _makeLeadsMembers = false;

  public Group(long groupId, long subscriptionId, String displayName, List<Long> groupLeadIds) {
    _groupId = groupId;
    _subscriptionId = subscriptionId;
    _displayName = displayName;
    _groupLeadIds = groupLeadIds;
  }

  public Group(AccountDbInfo accountDb, JSONObject group) {
    parseMutableFields(group);
    _groupId = new SubscriptionManager(accountDb).getNextGroupId();
  }

  public Group(String groupId, JSONObject group) {
    parseMutableFields(group);
    _groupId = Long.parseLong(groupId);
  }

  private void parseMutableFields(JSONObject group) {
    _subscriptionId = group.getLong("subscriptionId");
    _displayName = group.getString("displayName");
    if (_displayName == null || _displayName.isBlank()) {
      throw new JSONException("group name cannot be empty");
    }
    _groupLeadIds = new ArrayList<>();
    JSONArray leadsJson = group.getJSONArray("groupLeadIds");
    for (int i = 0; i < leadsJson.length(); i++) {
      _groupLeadIds.add(leadsJson.getLong(i));
    }
    _makeLeadsMembers = group.getBoolean("makeLeadsMembers");
  }

  public long getGroupId() {
    return _groupId;
  }

  public long getSubscriptionId() {
    return _subscriptionId;
  }

  public String getDisplayName() {
    return _displayName;
  }

  public List<Long> getGroupLeadIds() {
    return _groupLeadIds;
  }

  // this represents an extra optional action; on incoming groups only
  public boolean makeLeadsMembers() {
    return _makeLeadsMembers;
  }

  public JSONObject toJson() {
    return new JSONObject()
        .put("groupId", _groupId)
        .put("subscriptionId", _subscriptionId)
        .put("displayName", _displayName)
        .put("groupLeadIds", new JSONArray(_groupLeadIds));
  }

}
