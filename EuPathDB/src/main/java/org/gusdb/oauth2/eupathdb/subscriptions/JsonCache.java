package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

public class JsonCache {

  private static final long CACHE_DURATION_MS = 5 /* minutes */ * 60 * 1000;

  /************ Subscriptions JSON Cache ************/

  private static volatile JsonCache SUBSCRIPTIONS_JSON = new JsonCache();
  
  public static String getSubscriptionJson(Supplier<String> fetcher) {
    return SUBSCRIPTIONS_JSON.getData(fetcher);
  }

  public static void expireSubscriptionsJson() {
    SUBSCRIPTIONS_JSON.expire();
  }

  /*************** Groups JSON Caches ***************/

  private static ConcurrentMap<GroupFilter,JsonCache> GROUPS_JSON_MAP = new ConcurrentHashMap<>() {{
    for (GroupFilter filter : GroupFilter.values()) {
      put(filter, new JsonCache());
    }
  }};

  public static String getGroupsJson(GroupFilter groupFilter, Supplier<String> fetcher) {
    return GROUPS_JSON_MAP.get(groupFilter).getData(fetcher);
  }

  public static void expireGroupsJson() {
    GROUPS_JSON_MAP.values().stream().forEach(JsonCache::expire);
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
