package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

import org.gusdb.oauth2.eupathdb.AccountDbInfo;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Subscription {

  // special values for last_active_year
  public static final int NEVER_SUBSCRIBED = 0;
  public static final int NEVER_EXPIRES = 9999;
  private static final int EARLIEST_SUBSCRIPTION_YEAR = 2025;

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
  private String _displayName;
  private int _lastActiveYear;
  private boolean _isActive;

  public Subscription(long subscriptionId, String displayName, int lastActiveYear) {
    _subscriptionId = subscriptionId;
    _displayName = displayName;
    _lastActiveYear = lastActiveYear;
    _isActive = isActive(_lastActiveYear);
  }

  public Subscription(AccountDbInfo accountDb, JSONObject subscription) {
    parseMutableFields(subscription);
    _subscriptionId = new SubscriptionManager(accountDb).getNextSubscriptionId();
  }

  public Subscription(String subscriptionId, JSONObject subscription) {
    parseMutableFields(subscription);
    _subscriptionId = Long.parseLong(subscriptionId);
  }

  public static boolean isValidLastActiveYear(int value) {
    return value == NEVER_SUBSCRIBED ||
        (value >= EARLIEST_SUBSCRIPTION_YEAR && value <= NEVER_EXPIRES);
  }

  private void parseMutableFields(JSONObject subscription) {
    _displayName = subscription.getString("displayName");
    if (_displayName == null || _displayName.isBlank()) {
      throw new JSONException("subscription display name cannot be empty");
    }
    _lastActiveYear = subscription.getInt("lastActiveYear");
    if (!isValidLastActiveYear(_lastActiveYear)) {
      throw new JSONException("Invalid lastActiveYear value (" + _lastActiveYear + ")");
    }
    _isActive = isActive(_lastActiveYear);
  }

  private boolean isActive(int lastActiveYear) {
    int currentYear = Calendar.getInstance().get(Calendar.YEAR);
    return lastActiveYear >= currentYear; 
  }

  public long getSubscriptionId() {
    return _subscriptionId;
  }

  public int getLastActiveYear() {
    return _lastActiveYear;
  }

  public String getDisplayName() {
    return _displayName;
  }

  public JSONObject toJson() {
    return new JSONObject()
        .put("subscriptionId", _subscriptionId)
        .put("isActive", _isActive)
        .put("lastActiveYear", _lastActiveYear)
        .put("displayName", _displayName);
  }
}
