--liquibase formatted sql

--changeset fynza:049-shipping-views runOnChange:true splitStatements:false
CREATE OR REPLACE VIEW shipment_tracking_view AS
SELECT
    s.public_id                                     AS shipment_id,
    s.shipment_number,
    s.tracking_number,
    s.status                                        AS shipment_status,
    te.status                                       AS last_tracking_status,
    te.description                                  AS last_event_description,
    te.location                                     AS last_event_location,
    te.occurred_at                                  AS last_event_at,
    s.estimated_delivery_date,
    s.actual_delivery_date,
    s.updated_at                                    AS shipment_updated_at
FROM shipments s
LEFT JOIN LATERAL (
    SELECT status, description, location, occurred_at
    FROM tracking_events te2
    WHERE te2.shipment_id = s.id
    ORDER BY te2.occurred_at DESC
    LIMIT 1
) te ON TRUE;

--changeset fynza:049-shipping-views-seller-dashboard runOnChange:true splitStatements:false
CREATE OR REPLACE VIEW seller_shipment_dashboard_view AS
SELECT
    f.seller_id,
    s.status,
    COUNT(*)                                        AS shipment_count,
    SUM(s.shipping_cost)                            AS total_shipping_cost,
    DATE_TRUNC('day', s.created_at)                 AS shipment_date
FROM shipments s
JOIN fulfillments f ON f.id = s.fulfillment_id
GROUP BY f.seller_id, s.status, DATE_TRUNC('day', s.created_at);

--changeset fynza:049-shipping-views-exception runOnChange:true splitStatements:false
CREATE OR REPLACE VIEW shipping_exception_view AS
SELECT
    se.public_id                                    AS exception_id,
    se.shipment_id,
    s.shipment_number,
    s.tracking_number,
    se.type                                         AS exception_type,
    se.severity,
    se.status                                       AS exception_status,
    se.description,
    se.detected_at,
    se.resolved_at,
    se.resolved_by
FROM shipment_exceptions se
JOIN shipments s ON s.public_id = se.shipment_id
WHERE se.is_active = TRUE;
