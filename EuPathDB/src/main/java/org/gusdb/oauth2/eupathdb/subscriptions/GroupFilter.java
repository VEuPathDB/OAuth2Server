package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.Calendar;
import java.util.function.Supplier;

public enum GroupFilter {

  ACTIVE_ONLY(() -> Calendar.getInstance().get(Calendar.YEAR)),
  ACTIVE_AND_EXPIRED(() -> Subscription.EARLIEST_SUBSCRIPTION_YEAR),
  ALL_GROUPS(() -> Subscription.NEVER_SUBSCRIBED);

  private final Supplier<Integer> _minLastActiveYearSupplier;

  private GroupFilter(Supplier<Integer> minLastActiveYearSupplier) {
    _minLastActiveYearSupplier = minLastActiveYearSupplier;
  }

  public int getMinLastActiveYear() {
    return _minLastActiveYearSupplier.get();
  }

}
