package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.Calendar;
import java.util.List;

import org.gusdb.fgputil.Tuples.TwoTuple;

public enum ActiveStatus {

  NEVER_SUBSCRIBED,
  ACTIVE,
  GRACE_PERIOD,
  EXPIRED;

  private static class GracePeriod extends TwoTuple<Integer,Integer> {
    public GracePeriod(int year, int daysInGracePeriod) {
      super(year, daysInGracePeriod);
    }
  }

  private static final List<GracePeriod> GRACE_PERIODS = List.of(
    new GracePeriod(2026,31+15) // Feb 15
  );

  public static ActiveStatus getActiveStatus(int lastActiveYear) {

    // first handle never subscribed
    if (lastActiveYear == Subscription.NEVER_SUBSCRIBED) {
      return ActiveStatus.NEVER_SUBSCRIBED;
    }

    // find current date
    Calendar cal = Calendar.getInstance();
    int currentYear = cal.get(Calendar.YEAR);
    int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

    // check if currently active
    if (lastActiveYear >= currentYear) {
      return ActiveStatus.ACTIVE;
    }

    // calculate grace period
    int gracePeriodDays = GRACE_PERIODS.stream()
        .filter(gp -> gp.getKey() == currentYear)  // find grace period for this year
        .findFirst()                               // look for result
        .map(gp -> gp.getSecond())                 // get grace period days if found
        .orElse(0);                                // otherwise, no grace period

    // in grace period if expired last year but in grace period window for this year
    if ((lastActiveYear == currentYear - 1) && (dayOfYear <= gracePeriodDays)) {
      return ActiveStatus.GRACE_PERIOD;
    }

    // otherwise expired
    return ActiveStatus.EXPIRED;
  }

}
