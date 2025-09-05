select g.subscription_token, g.group_name, p.first_name, p.last_name, p.organization
from (
    select l.user_id, s.*
    from (
      select g.subscription_token, g.group_name, g.group_id
      from $$accountschema$$subscription_groups g, $$accountschema$$subscriptions s
      where g.subscription_id = s.subscription_id
      and s.is_active = 1
    ) s
    left join $$accountschema$$subscription_group_leads l
    on l.group_id = s.group_id
) g
left join (
    select
      user_id,
      max(case when key = 'first_name' then value end) as first_name,
      max(case when key = 'last_name' then value end) as last_name,
      max(case when key = 'organization' then value end) as organization
    from $$accountschema$$account_properties
    group by user_id
) p
on g.user_id = p.user_id
order by g.group_name asc
