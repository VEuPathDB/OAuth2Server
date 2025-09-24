SELECT
    u.user_id,
    first_name || ' ' || last_name as name,
    email,
    to_char(u.last_login,'YYYY-MM-DD') as last_login,
    country,
    organization,
    organization_type,
    position,
    p.group_name as entered_group_name,
    group_type as entered_group_type,
    g.group_name,
    s.display_name as subscription_name,
    s.is_active as has_invoice,
    case
      when l.group_id is not null then 1
      when l.group_id is null then 0
    end as is_lead
FROM $$accountschema$$accounts u
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
) p
ON u.user_id = p.user_id
LEFT JOIN $$accountschema$$subscription_groups g
ON p.subscription_token = g.subscription_token
LEFT JOIN $$accountschema$$subscriptions s
ON g.subscription_id = s.subscription_id
LEFT JOIN $$accountschema$$subscription_group_leads l
ON l.group_id = g.group_id and l.user_id = u.user_id
WHERE g.group_id != 133570 -- VEuPathDB staff
AND u.last_login is not null
AND u.last_login > TRUNC(SYSDATE) - interval '3' year
AND u.email not like '%mailinator.com'
ORDER BY subscription_name asc, group_name asc, organization asc, name asc
