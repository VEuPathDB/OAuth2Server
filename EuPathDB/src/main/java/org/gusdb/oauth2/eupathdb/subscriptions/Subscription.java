package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.List;
import java.util.stream.Collectors;

import org.gusdb.oauth2.eupathdb.AccountDbInfo;
import org.json.JSONArray;
import org.json.JSONObject;

public class Subscription {

  public static class SubscriptionWithGroups {

    private Subscription _subscription;
    private List<Group> _groups;

    public SubscriptionWithGroups(Subscription subscription, List<Group> groups) {
      _subscription = subscription;
      _groups = groups;
    }

    public JSONObject toJson() {
      return _subscription.toJson()
          .put("groups", new JSONArray(_groups.stream()
              .map(Group::toJson).collect(Collectors.toList())));
    }
  }

  private long _subscriptionId;
  private boolean _isActive;
  private String _displayName;

  public Subscription(long subscriptionId, boolean isActive, String displayName) {
    _subscriptionId = subscriptionId;
    _isActive = isActive;
    _displayName = displayName;
  }

  public Subscription(AccountDbInfo accountDb, JSONObject subscription) {
    parseMutableFields(subscription);
    _subscriptionId = new SubscriptionManager(accountDb).getNextSubscriptionId();
  }

  public Subscription(String subscriptionId, JSONObject subscription) {
    parseMutableFields(subscription);
    _subscriptionId = Long.parseLong(subscriptionId);
  }

  private void parseMutableFields(JSONObject subscription) {
    _isActive = subscription.getBoolean("isActive");
    _displayName = subscription.getString("displayName");
  }

  public long getSubscriptionId() {
    return _subscriptionId;
  }

  public boolean isActive() {
    return _isActive;
  }

  public String getDisplayName() {
    return _displayName;
  }

  public JSONObject toJson() {
    return new JSONObject()
        .put("subscriptionId", _subscriptionId)
        .put("isActive", _isActive)
        .put("displayName", _displayName);
  }
}
