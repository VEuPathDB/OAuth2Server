package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.function.Supplier;

public class JsonCache {

  private static final long CACHE_DURATION_MS = 5 /* minutes */ * 60 * 1000;

  /************ Subscriptions JSON Cache ************/

  private static volatile JsonCache SUBSCRIPTIONS_ENTRY = new JsonCache();
  
  public static String getSubscriptionJson(Supplier<String> fetcher) {
    return SUBSCRIPTIONS_ENTRY.getData(fetcher);
  }

  public static void expireSubscriptionsJson() {
    SUBSCRIPTIONS_ENTRY.expire();
  }

  /*************** Groups JSON Caches ***************/

  private static volatile JsonCache GROUPS_WITH_INACTIVE_ENTRY = new JsonCache();
  private static volatile JsonCache GROUPS_WITHOUT_INACTIVE_ENTRY = new JsonCache();

  public static String getGroupsJson(boolean includingInactiveGroups, Supplier<String> fetcher) {
    return (includingInactiveGroups
        ? GROUPS_WITH_INACTIVE_ENTRY
        : GROUPS_WITHOUT_INACTIVE_ENTRY
    ).getData(fetcher);
  }

  public static void expireGroupsJson() {
    GROUPS_WITH_INACTIVE_ENTRY.expire();
    GROUPS_WITHOUT_INACTIVE_ENTRY.expire();
  }

  /*************** Cache object data/methods ***************/

  private volatile Long _lastWriteTime;
  private volatile String _cachedString;

  private synchronized String getData(Supplier<String> fetcher) {
    // check if empty or expired
    if (_cachedString == null || _lastWriteTime < System.currentTimeMillis() - CACHE_DURATION_MS) {
      // fill cache and set fill timestamp
      _cachedString = fetcher.get();
      _lastWriteTime = System.currentTimeMillis();
    }
    return _cachedString;
  }

  private synchronized void expire() {
    _cachedString = null;
  }
}
