package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.Calendar;
import java.util.List;

import org.gusdb.fgputil.Tuples.TwoTuple;

public class ActiveStatus {

  private static class GracePeriod extends TwoTuple<Integer,Integer> {
    public GracePeriod(int year, int daysInGracePeriod) {
      super(year, daysInGracePeriod);
    }
  }

  private static final List<GracePeriod> GRACE_PERIODS = List.of(
    new GracePeriod(2026,31+15) // Feb 15
  );

  public static boolean isActive(int lastActiveYear) {
    return lastActiveYear >= Calendar.getInstance().get(Calendar.YEAR);
  }

  public static boolean isInGracePeriod(int lastActiveYear) {

    // find current date
    Calendar cal = Calendar.getInstance();
    int currentYear = cal.get(Calendar.YEAR);
    int dayOfYear = cal.get(Calendar.DAY_OF_YEAR);

    if (lastActiveYear >= currentYear) {
      return true; // subscription still active
    }
    if (lastActiveYear < currentYear - 1) {
      return false; // subscription expired over a year ago
    }

    // otherwise, may be in grace period
    int gracePeriodDays = GRACE_PERIODS.stream()
        .filter(gp -> gp.getKey() == currentYear)  // find grace period for this year
        .findFirst()                               // look for result
        .map(gp -> gp.getSecond())                 // get grace period days if found
        .orElse(0);                                // otherwise, no grace period

    return dayOfYear <= gracePeriodDays;
  }

}
