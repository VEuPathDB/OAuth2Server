package org.gusdb.oauth2.eupathdb;

import java.util.HashSet;
import java.util.Set;

import org.gusdb.oauth2.eupathdb.subscriptions.SubscriptionService;

public class ServiceApplication extends org.gusdb.oauth2.server.ServiceApplication {

  @Override
  public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.addAll(super.getClasses());
    classes.add(SubscriptionService.class);
    return classes;
  }
}
