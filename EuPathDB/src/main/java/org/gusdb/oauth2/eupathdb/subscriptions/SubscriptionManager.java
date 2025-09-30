package org.gusdb.oauth2.eupathdb.subscriptions;

import java.util.List;

import org.gusdb.fgputil.db.pool.DatabaseInstance;

public class SubscriptionManager {

  private final DatabaseInstance _db;
  private final String _accountSchema;

  public SubscriptionManager(DatabaseInstance db, String accountSchema) {
    _db = db;
    _accountSchema = accountSchema;
  }

  public List<Subscription> getSubscriptions() {
    // TODO Auto-generated method stub
    return null;
  }

  public void addSubscription(Subscription subscriber) {
    // TODO Auto-generated method stub
    
  }

  public Subscription getSubscription(long parseLong) {
    // TODO Auto-generated method stub
    return null;
  }

  public void updateSubscription(Subscription subscriber) {
    // TODO Auto-generated method stub
    
  }

  public void addGroup(Group group) {
    // TODO Auto-generated method stub
    
  }

  public Group getGroup(long parseLong) {
    // TODO Auto-generated method stub
    return null;
  }

  public void updateGroup(Group group) {
    // TODO Auto-generated method stub
    
  }

}
