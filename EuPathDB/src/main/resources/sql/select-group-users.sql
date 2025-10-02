WITH users AS (
  SELECT
    u1.user_id,
    first_name || ' ' || last_name as name,
    organization,
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
        'organization' as organization,
        'subscription_token' as subscription_token
      )
    )
  ) up
  ON u1.user_id = up.user_id
)
SELECT u.*, 0 as lead
  FROM users u
  WHERE subscription_token = ?
UNION ALL
SELECT u.*, 1 as lead
  FROM users u
  WHERE user_id in ( $$userids$$ )
