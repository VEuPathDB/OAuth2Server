package org.gusdb.oauth2.eupathdb.subscriptions;

import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.sql.DataSource;
import javax.ws.rs.NotFoundException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.gusdb.fgputil.Tuples.TwoTuple;
import org.gusdb.fgputil.db.platform.DBPlatform;
import org.gusdb.fgputil.db.runner.SQLRunner;
import org.gusdb.oauth2.eupathdb.AccountDbInfo;
import org.gusdb.oauth2.eupathdb.subscriptions.Group.GroupWithUsers;
import org.gusdb.oauth2.eupathdb.subscriptions.Group.SimpleUser;
import org.gusdb.oauth2.eupathdb.subscriptions.Subscription.SubscriptionWithGroups;

public class SubscriptionManager {

  private static final Logger LOG = LogManager.getLogger(SubscriptionManager.class);

  private static final String GROUP_USERS_SQL = BulkDataDumper.readResourceSql("sql/select-group-users.sql");

  private static final String SCHEMA_MACRO = "$$accountschema$$";

  private final DataSource _ds;
  private final DBPlatform _platform;
  private final String _schema;

  public SubscriptionManager(AccountDbInfo accountDb) {
    _ds = accountDb.DATASOURCE;
    _platform = accountDb.DB.getPlatform();
    _schema = accountDb.SCHEMA;
  }

  public List<Subscription> getSubscriptions() {
    String sql = (
        "select subscription_id, is_active, display_name " +
        "from " + SCHEMA_MACRO + "subscriptions s " +
        "order by display_name"
    ).replace(SCHEMA_MACRO, _schema);
    return new SQLRunner(_ds, sql).executeQuery(rs -> {
      List<Subscription> subs = new ArrayList<>();
      while (rs.next()) {
        //subs.add(new Subscription(
        //    rs.getLong("subscription_id"),
        //    rs.getBoolean("is_active"),
        //    rs.getString("display_name")));
      }
      return subs;
    });
  }

  public void addSubscription(Subscription subscription) {
    LOG.info("Inserting new subscription: " + subscription.toJson().toString());
    String sql = (
        "insert into " + SCHEMA_MACRO + "subscriptions " +
        "(subscription_id, is_active, display_name) values (?, ?, ?)"
    ).replace(SCHEMA_MACRO, _schema);
    new SQLRunner(_ds, sql).executeStatement(
        new Object[] {
            subscription.getSubscriptionId(),
            _platform.convertBoolean(subscription.isActive()),
            subscription.getDisplayName()
        },
        new Integer[] {
            Types.BIGINT,
            _platform.getBooleanType(),
            Types.VARCHAR
        }
    );
  }

  public SubscriptionWithGroups getSubscription(long subscriptionId) {
    String sql = (
        "select s.subscription_id, s.is_active, s.display_name, g.group_id, g.group_name, l.user_id " +
        "from " + SCHEMA_MACRO +"subscriptions s " +
        "left join " + SCHEMA_MACRO + "subscription_groups g " +
        "on s.subscription_id = g.subscription_id " +
        "left join " + SCHEMA_MACRO + "subscription_group_leads l " +
        "on l.group_id = g.group_id " +
        "where s.subscription_id = ? " +
        "order by group_id"
    ).replace(SCHEMA_MACRO, _schema);
    Optional<SubscriptionWithGroups> result = new SQLRunner(_ds, sql).executeQuery(
        new Object[] { subscriptionId },
        new Integer[] { Types.BIGINT },
        rs -> {
          if (!rs.next()) {
            return Optional.empty();
          }
          Subscription sub = new Subscription(
              rs.getLong("subscription_id"),
              rs.getBoolean("is_active"),
              rs.getString("display_name"));
          List<Group> groups = new ArrayList<>();

          // allow subscriptions with no assigned groups; if group_id in the first row is null, no groups
          rs.getLong("group_id");
          if (rs.wasNull()) {
            return Optional.of(new SubscriptionWithGroups(sub, groups));
          }

          // at least one group; process them
          do {
            List<Long> leads = new ArrayList<>();
            Group group = new Group(
                rs.getLong("group_id"),
                rs.getLong("subscription_id"),
                rs.getString("group_name"),
                leads);
            groups.add(group);
            do {
              Long lead = rs.getLong("user_id");
              if (!rs.wasNull()) leads.add(lead);
              if (!rs.next()) {
                return Optional.of(new SubscriptionWithGroups(sub, groups));
              }
            }
            while (rs.getLong("group_id") == group.getGroupId());
          }
          while (true);
        }
    );
    return result.orElseThrow(() -> new NotFoundException());
  }

  public void updateSubscription(Subscription subscription) {
    LOG.info("Updating subscription: " + subscription.toJson().toString());
    String sql = (
        "update " + SCHEMA_MACRO + "subscriptions set is_active = ?, display_name = ? where subscription_id = ?"
    ).replace(SCHEMA_MACRO, _schema);
    boolean updated = 0 < new SQLRunner(_ds, sql).executeUpdate(
        new Object[] {
            _platform.convertBoolean(subscription.isActive()),
            subscription.getDisplayName(),
            subscription.getSubscriptionId()
        },
        new Integer[] {
            _platform.getBooleanType(),
            Types.VARCHAR,
            Types.BIGINT
        }
    );
    if (!updated) {
      throw new NotFoundException();
    }
  }

  public void addGroup(Group group, String subscriptionToken) {
    LOG.info("Inserting new group: " + group.toJson().toString());

    // 1. insert the group
    String sql = (
        "insert into " + SCHEMA_MACRO + "subscription_groups " +
        "(group_id, subscription_id, group_name, subscription_token) values (?, ?, ?, ?)"
    ).replace(SCHEMA_MACRO, _schema);
    new SQLRunner(_ds, sql).executeStatement(
        new Object[] {
            group.getGroupId(),
            group.getSubscriptionId(),
            group.getDisplayName(),
            subscriptionToken
        },
        new Integer[] {
            Types.BIGINT,
            Types.BIGINT,
            Types.VARCHAR,
            Types.VARCHAR
        }
    );

    // 2. insert leads, if any
    insertGroupLeads(group);
  }

  public void updateGroup(Group group) {
    LOG.info("Updating group: " + group.toJson().toString());

    // 1. update the group
    String sql = (
        "update " + SCHEMA_MACRO + "subscription_groups set subscription_id = ?, group_name = ? where group_id = ?"
    ).replace(SCHEMA_MACRO, _schema);
    new SQLRunner(_ds, sql).executeStatement(
        new Object[] {
            group.getSubscriptionId(),
            group.getDisplayName(),
            group.getGroupId()
        },
        new Integer[] {
            Types.BIGINT,
            Types.VARCHAR,
            Types.BIGINT
        }
    );

    // 2. remove any existing group leads (rather than reconcile)
    String removeSql = (
        "delete from " + SCHEMA_MACRO + "subscription_group_leads where group_id = ?"
    ).replace(SCHEMA_MACRO, _schema);
    new SQLRunner(_ds, removeSql).executeStatement(
        new Object[] { group.getGroupId() },
        new Integer[] { Types.BIGINT }
    );

    // 3. insert leads, if any
    insertGroupLeads(group);
  }

  private void insertGroupLeads(Group group) {
    String leadSql = (
        "insert into " + SCHEMA_MACRO + "subscription_group_leads " +
        "(group_id, user_id) values (?, ?)"
    ).replace(SCHEMA_MACRO, _schema);
    for (Long leadUserId : group.getGroupLeadIds()) {
      new SQLRunner(_ds, leadSql).executeStatement(
          new Object[] { group.getGroupId(), leadUserId },
          new Integer[] { Types.BIGINT, Types.BIGINT }
      );
    }
  }

  public GroupWithUsers getGroup(long groupId) {

    // 1. Fill in group
    String groupSql = (
        "select s.subscription_id, s.is_active, s.display_name, g.group_id, g.group_name, g.subscription_token, l.user_id " +
        "from " + SCHEMA_MACRO + "subscriptions s, " + SCHEMA_MACRO + "subscription_groups g " +
        "left join " + SCHEMA_MACRO + "subscription_group_leads l " +
        "on l.group_id = g.group_id " +
        "where s.subscription_id = g.subscription_id " +
        "and g.group_id = ?"
    ).replace(SCHEMA_MACRO, _schema);
    TwoTuple<Group,String> group = new SQLRunner(_ds, groupSql).executeQuery(
        new Object[] { groupId },
        new Integer[] { Types.BIGINT },
        rs -> {
          if (!rs.next()) throw new NotFoundException();
          List<Long> leadIds = new ArrayList<>();
          Group g = new Group(
              rs.getLong("group_id"),
              rs.getLong("subscription_id"),
              rs.getString("group_name"),
              leadIds);
          String subscriptionToken = rs.getString("subscription_token");
          boolean done = false;
          do {
            Long leadId = rs.getLong("user_id");
            if (!rs.wasNull()) leadIds.add(leadId);
            if (!rs.next()) done = true;
          }
          while (!done);
          return new TwoTuple<>(g, subscriptionToken);
        }
    );

    // 2. Look up users associated with the group
    String leadIdsStr = group.getFirst().getGroupLeadIds().stream()
        .map(l -> String.valueOf(l)).collect(Collectors.joining(", "));
    String sql = GROUP_USERS_SQL
        .replace(SCHEMA_MACRO, _schema)
        .replace("$$userids$$", leadIdsStr);
    return new SQLRunner(_ds, sql).executeQuery(
        new Object[] { group.getSecond() }, // subscription token
        new Integer[] { Types.VARCHAR },
        rs -> {
          List<SimpleUser> members = new ArrayList<>();
          List<SimpleUser> leads = new ArrayList<>();
          while (rs.next()) {
            SimpleUser user = new SimpleUser(
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("organization")
            );
            (rs.getBoolean("lead") ? leads : members).add(user);
          }
          return new GroupWithUsers(group.getFirst(), group.getSecond(), leads, members);
        }
    );
  }

  public long getNextSubscriptionId() {
    return nextIdFromSequence("subscriptions");
  }

  public long getNextGroupId() {
    return nextIdFromSequence("subscription_groups");
  }

  private long nextIdFromSequence(String tableName) {
    try {
      return _platform.getNextId(_ds, _schema, tableName);
    }
    catch (SQLException e) {
      throw new RuntimeException("Could not retrieve next ID for table " + tableName);
    }
  }

}
