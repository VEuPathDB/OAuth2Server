SELECT
  u.user_id,
  name,
  email,
  last_login
  country,
  organization,
  organization_type,
  position,
  entered_group_name,
  entered_group_type,
  g.group_name,
  g.subscription_name,
  g.is_active,
  CASE
    WHEN g.is_active is null THEN null
    WHEN l.group_id is not null THEN 1
    WHEN l.group_id is null THEN 0
  END as is_lead
FROM (
  SELECT
    u1.user_id,
    first_name || ' ' || last_name as name,
    email,
    to_char(last_login,'YYYY-MM-DD') as last_login,
    country,
    organization,
    organization_type,
    position,
    group_name as entered_group_name,
    group_type as entered_group_type,
    subscription_token
  FROM $$accountschema$$accounts u1
  LEFT JOIN (
    SELECT *
    FROM $$accountschema$$account_properties
    PIVOT (
      max(value)
      for key in (
        'first_name' as first_name,
        'last_name' as last_name,
        'country' as country,
        'organization' as organization,
        'organization_type' as organization_type,
        'group_name' as group_name,
        'group_type' as group_type,
        'position' as position,
        'subscription_token' as subscription_token
      )
    )
  ) up
  ON u1.user_id = up.user_id
  WHERE u1.last_login is not null -- never logged in
  AND u1.last_login > TRUNC(SYSDATE) - interval '3' year
  AND u1.email not like '%mailinator.com' -- test accounts
  AND up.country is not null -- came back after 9/24 downtime
) u
LEFT JOIN (
  SELECT
    g1.group_id,
    g1.group_name,
    g1.subscription_token,
    s.display_name as subscription_name,
    s.is_active
  FROM $$accountschema$$subscription_groups g1, $$accountschema$$subscriptions s
  WHERE g1.subscription_id = s.subscription_id
  AND (g1.group_id is not null or g1.group_id != 133570) -- VEuPathDB staff
) g
ON u.subscription_token = g.subscription_token
LEFT JOIN $$accountschema$$subscription_group_leads l
ON l.group_id = g.group_id and l.user_id = u.user_id
ORDER BY subscription_name asc, group_name asc, name asc
