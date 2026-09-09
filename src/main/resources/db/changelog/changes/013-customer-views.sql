--liquibase formatted sql
--changeset fynza:013-customer-views dbms:postgresql

CREATE VIEW v_customer_summary AS
SELECT
    c.id,
    c.public_id,
    c.user_id,
    c.customer_number,
    c.status,
    c.is_active,
    c.created_at,
    c.updated_at,
    u.first_name,
    u.last_name,
    u.email,
    u.phone
FROM customers c
JOIN users u ON u.id = c.user_id;

CREATE VIEW v_customer_detail AS
SELECT
    c.id,
    c.public_id,
    c.user_id,
    c.customer_number,
    c.status,
    c.is_active,
    c.created_at,
    c.updated_at,
    u.first_name,
    u.last_name,
    u.email,
    u.phone,
    cp.public_id  AS pref_public_id,
    cp.language,
    cp.currency,
    cp.marketing_opt_in,
    cp.email_notifications,
    cp.sms_notifications,
    cp.push_notifications,
    cp.updated_at AS pref_updated_at
FROM customers c
JOIN      users u               ON u.id           = c.user_id
LEFT JOIN customer_preferences cp ON cp.customer_id = c.id;

CREATE VIEW v_customer_stats AS
SELECT
    1                                                                  AS singleton,
    COUNT(*)                                                           AS total_customers,
    COUNT(*) FILTER (WHERE c.status = 'ACTIVE')                        AS active_customers,
    COUNT(*) FILTER (WHERE c.created_at >= date_trunc('month', now())) AS new_customers_this_month
FROM customers c
WHERE c.is_active = TRUE;

--rollback DROP VIEW IF EXISTS v_customer_stats;
--rollback DROP VIEW IF EXISTS v_customer_detail;
--rollback DROP VIEW IF EXISTS v_customer_summary;
