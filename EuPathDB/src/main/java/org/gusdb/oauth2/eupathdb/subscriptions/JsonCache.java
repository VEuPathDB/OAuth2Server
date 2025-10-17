package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.function.Supplier;

public class JsonCache {

  private static final long CACHE_DURATION_MS = 5 /* minutes */ * 60 * 1000;

  /************ Subscriptions JSON Cache ************/
  
  private static volatile long LAST_SUBSCRIPTIONS_WRITE_TIME;
  private static volatile String CACHED_SUBSCRIPTIONS_STRING;
  
  public static synchronized String getSubscriptionJson(Supplier<String> fetcher) {
    if (CACHED_SUBSCRIPTIONS_STRING == null || LAST_SUBSCRIPTIONS_WRITE_TIME < System.currentTimeMillis() - CACHE_DURATION_MS) {
      CACHED_SUBSCRIPTIONS_STRING = fetcher.get();
      LAST_SUBSCRIPTIONS_WRITE_TIME = System.currentTimeMillis();
    }
    return CACHED_SUBSCRIPTIONS_STRING;
  }

  public static synchronized void expireSubscriptionsJson() {
    CACHED_SUBSCRIPTIONS_STRING = null;
  }

  /*************** Groups JSON Cache ***************/

  private static volatile long LAST_GROUPS_WRITE_TIME;
  private static volatile String CACHED_GROUPS_STRING;

  public static synchronized String getGroupsJson(Supplier<String> fetcher) {
    if (CACHED_GROUPS_STRING == null || LAST_GROUPS_WRITE_TIME < System.currentTimeMillis() - CACHE_DURATION_MS) {
      CACHED_GROUPS_STRING = fetcher.get();
      LAST_GROUPS_WRITE_TIME = System.currentTimeMillis();
    }
    return CACHED_GROUPS_STRING;
  }

  public static synchronized void expireGroupsJson() {
    CACHED_GROUPS_STRING = null;
  }
}
