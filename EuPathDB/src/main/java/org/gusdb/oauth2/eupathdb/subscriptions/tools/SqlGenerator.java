package org.gusdb.oauth2.eupathdb.subscriptions.tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;

import org.gusdb.oauth2.eupathdb.tools.SubscriptionTokenGenerator;

public class SqlGenerator {

  public static void main(String[] args) throws Exception {
    try (BufferedReader br = new BufferedReader(new FileReader("/home/rdoherty/Downloads/updates-2025-09-26.tsv", StandardCharsets.UTF_8))) {
      String[] cols = br.readLine().split("\t");
      System.out.println("-- " + String.join(", ", cols));
      int rows = 0;
      long nextSubscriptionId = 166420;
      long nextGroupId = 166240;
      while (br.ready()) {
        cols = br.readLine().split("\t");
        long userId = Long.parseLong(cols[0]);
        String groupName = cols[9];
        String subscriberName = cols[10];
        String status = cols[13];

        // validate old group name
        String oldGroupName = cols.length < 15 ? null : cols[14];
        if (oldGroupName != null && oldGroupName.isBlank()) oldGroupName = null;
        if (status.equals("edited") && oldGroupName == null) throw new RuntimeException("edited status but no new group name present");
        if (!status.equals("edited") && oldGroupName != null) throw new RuntimeException("non-edited status but new group name present");
        if (oldGroupName == null) oldGroupName = groupName;

        String[] fields = new String[] { String.valueOf(userId), groupName, subscriberName, status, oldGroupName };

        /*
edited
- edit group_name to new name
- edit subscriber_name to new name
- assign has_invoice = 1
- (maybe) assign row as lead

existing
- group_name already present
- assign has_invoice = 1
- (maybe) assign row as lead

new
- new or existing subscriber
- new group_clean
- assign has_invoice = 1
- assign user to group
- (maybe) assign row as lead
         */
        
        System.out.println("-- Row " + (++rows) + " <" + String.join("> <", fields) + ">");
        switch(status) {
          case "edited":
            System.out.println("update useraccounts.subscription_groups set group_name = '" + groupName + "' where group_name = '" + oldGroupName + "';");
            System.out.println("update useraccounts.subscriptions set is_active = 1, display_name = '" + subscriberName + "' where subscription_id in (select subscription_id from useraccounts.subscription_groups where group_name = '" + groupName + "');");
            System.out.println("insert into useraccounts.subscription_group_leads (group_id, user_id) values ((select group_id from useraccounts.subscription_groups where group_name = '" + groupName +"'), " + userId + ");");
            break;
          case "existing":
            System.out.println("update useraccounts.subscriptions set is_active = 1, display_name = '" + subscriberName + "' where subscription_id in (select subscription_id from useraccounts.subscription_groups where group_name = '" + groupName + "');");
            System.out.println("insert into useraccounts.subscription_group_leads (group_id, user_id) values ((select group_id from useraccounts.subscription_groups where group_name = '" + groupName +"'), " + userId + ");");
            break;
          case "new":
            String subscriptionToken = SubscriptionTokenGenerator.getNewToken();
            System.out.println("insert into useraccounts.subscriptions (subscription_id, is_active, display_name)" +
                " values (" + nextSubscriptionId + ", 1, '" + subscriberName + "');");
            System.out.println("insert into useraccounts.subscription_groups (group_id, subscription_id, group_name, subscription_token)" +
                " values (" + nextGroupId + ", " + nextSubscriptionId + ", '" + groupName + "', '" + subscriptionToken + "');");
            System.out.println("insert into useraccounts.subscription_group_leads (group_id, user_id)" +
                " values (" + nextGroupId + ", " + userId + ");");
            // make sure lead is member of the group
            System.out.println("insert into useraccounts.account_properties (user_id, key, value)" +
                " values (" + userId + ", 'subscription_token', '" + subscriptionToken + "');");
            // insert may fail; use update too
            System.out.println("update useraccounts.account_properites set value = '" + subscriptionToken + "' where user_id = " + userId + " and key = 'subscription_token';");
            break;
          default:
            throw new RuntimeException("Bad Status: " + status);
        }
      }
    }
  }
}
