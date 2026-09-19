--liquibase formatted sql

--changeset fynza:034-v-order-summary dbms:postgresql
-- One row per order. Aggregates item count, total quantity, and
-- seller count for list/dashboard reads without loading child collections.
CREATE VIEW v_order_summary AS
SELECT
    o.id,
    o.public_id,
    o.order_number,
    o.customer_id,
    o.status,
    o.payment_status,
    o.payment_method,
    o.coupon_code,
    o.subtotal,
    o.tax,
    o.shipping_cost,
    o.discount,
    o.total_amount,
    o.is_active,
    o.created_at,
    o.updated_at,
    COUNT(DISTINCT oi.id)::INTEGER       AS item_count,
    COALESCE(SUM(oi.quantity), 0)::INTEGER AS total_quantity,
    COUNT(DISTINCT so.seller_id)::INTEGER AS seller_count
FROM orders o
LEFT JOIN order_items   oi ON oi.order_id  = o.id
LEFT JOIN seller_orders so ON so.order_id  = o.id
GROUP BY o.id, o.public_id, o.order_number, o.customer_id, o.status,
         o.payment_status, o.payment_method, o.coupon_code, o.subtotal,
         o.tax, o.shipping_cost, o.discount, o.total_amount, o.is_active,
         o.created_at, o.updated_at;

--rollback DROP VIEW IF EXISTS v_order_summary;

--changeset fynza:034-v-order-detail dbms:postgresql
-- One row per order-item with full order and seller-order context.
-- Used for order detail pages and fulfilment views.
CREATE VIEW v_order_detail AS
SELECT
    o.id                  AS order_id,
    o.public_id           AS order_public_id,
    o.order_number,
    o.customer_id,
    o.status              AS order_status,
    o.payment_status,
    o.payment_method,
    o.coupon_code,
    o.subtotal            AS order_subtotal,
    o.tax                 AS order_tax,
    o.shipping_cost       AS order_shipping_cost,
    o.discount            AS order_discount,
    o.total_amount        AS order_total,
    o.shipping_name,
    o.shipping_phone,
    o.shipping_line1,
    o.shipping_line2,
    o.shipping_city,
    o.shipping_state,
    o.shipping_postal,
    o.shipping_country,
    o.is_active           AS order_is_active,
    o.created_at          AS ordered_at,

    so.id                 AS seller_order_id,
    so.public_id          AS seller_order_public_id,
    so.seller_id,
    so.status             AS seller_order_status,
    so.subtotal           AS seller_subtotal,
    so.tracking_number    AS seller_tracking_number,

    oi.id                 AS item_id,
    oi.public_id          AS item_public_id,
    oi.product_id,
    oi.variant_id,
    oi.product_name,
    oi.product_sku,
    oi.product_image_url,
    oi.quantity,
    oi.unit_price,
    oi.subtotal           AS item_subtotal
FROM orders o
JOIN seller_orders so ON so.order_id       = o.id
JOIN order_items   oi ON oi.seller_order_id = so.id;

--rollback DROP VIEW IF EXISTS v_order_detail;

--changeset fynza:034-v-product-summary dbms:postgresql
-- One row per product. Combines primary category and aggregate
-- inventory availability. Uses the post-018 products schema
-- (price/rating/media columns were moved to their own modules).
CREATE VIEW v_product_summary AS
SELECT
    p.id,
    p.is_active,
    p.created_at,
    p.updated_at,
    p.product_number,
    p.name,
    p.slug,
    p.brand,
    p.sku,
    p.description,
    p.status,
    p.product_type,
    p.visibility,
    p.store_id,
    p.seller_id,
    c.id              AS category_id,
    c.name            AS category_name,
    c.slug            AS category_slug,
    COALESCE(inv.on_hand_quantity,  0) AS on_hand_quantity,
    COALESCE(inv.reserved_quantity, 0) AS reserved_quantity,
    COALESCE(inv.on_hand_quantity - inv.reserved_quantity, 0) AS available_quantity,
    CASE
        WHEN COALESCE(inv.on_hand_quantity - inv.reserved_quantity, 0) <= 0 THEN 'OUT_OF_STOCK'
        WHEN COALESCE(inv.on_hand_quantity - inv.reserved_quantity, 0) <= 5 THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS computed_inventory_status
FROM products p
LEFT JOIN product_categories pc ON pc.product_id = p.id AND pc.is_primary = TRUE
LEFT JOIN categories          c  ON c.id          = pc.category_id
LEFT JOIN (
    SELECT
        product_id,
        SUM(on_hand_quantity)  AS on_hand_quantity,
        SUM(reserved_quantity) AS reserved_quantity
    FROM inventory
    WHERE is_active = TRUE
      AND variant_id IS NULL
    GROUP BY product_id
) inv ON inv.product_id = p.id;

--rollback DROP VIEW IF EXISTS v_product_summary;

--changeset fynza:034-v-inventory-summary dbms:postgresql
-- One row per inventory record with location context and a
-- computed stock_status label for operational stock views.
CREATE VIEW v_inventory_summary AS
SELECT
    i.id,
    i.public_id,
    i.product_id,
    i.variant_id,
    i.seller_id,
    i.store_id,
    i.on_hand_quantity,
    i.reserved_quantity,
    i.incoming_quantity,
    i.damaged_quantity,
    (i.on_hand_quantity - i.reserved_quantity) AS available_quantity,
    i.low_stock_threshold,
    i.allow_backorder,
    i.is_active,
    i.created_at,
    i.updated_at,
    il.id            AS location_id,
    il.name          AS location_name,
    il.code          AS location_code,
    il.location_type,
    il.status        AS location_status,
    CASE
        WHEN (i.on_hand_quantity - i.reserved_quantity) <= 0               THEN 'OUT_OF_STOCK'
        WHEN (i.on_hand_quantity - i.reserved_quantity) <= i.low_stock_threshold THEN 'LOW_STOCK'
        ELSE 'IN_STOCK'
    END AS stock_status
FROM inventory i
JOIN inventory_locations il ON il.id = i.location_id;

--rollback DROP VIEW IF EXISTS v_inventory_summary;

--changeset fynza:034-v-cart-summary dbms:postgresql
-- One row per cart. Aggregates line-item count and total quantity
-- for cart badge / admin dashboard reads.
CREATE VIEW v_cart_summary AS
SELECT
    c.id,
    c.public_id,
    c.user_id,
    c.cart_token,
    c.status,
    c.is_guest,
    c.coupon_code,
    c.subtotal,
    c.discount_amount,
    c.shipping_total,
    c.tax_total,
    c.grand_total,
    c.expires_at,
    c.created_at,
    c.updated_at,
    COALESCE(ci.item_count,  0)::INTEGER AS item_count,
    COALESCE(ci.total_qty,   0)::INTEGER AS total_quantity
FROM carts c
LEFT JOIN (
    SELECT cart_id,
           COUNT(*)      AS item_count,
           SUM(quantity) AS total_qty
    FROM cart_items
    GROUP BY cart_id
) ci ON ci.cart_id = c.id;

--rollback DROP VIEW IF EXISTS v_cart_summary;

--changeset fynza:034-v-wishlist-summary dbms:postgresql
-- One row per wishlist. Aggregates item count for list reads.
CREATE VIEW v_wishlist_summary AS
SELECT
    w.id,
    w.public_id,
    w.customer_id,
    w.name,
    w.description,
    w.status,
    w.visibility,
    w.is_default,
    w.created_at,
    w.updated_at,
    COALESCE(wi.item_count, 0)::INTEGER AS item_count
FROM wishlists w
LEFT JOIN (
    SELECT wishlist_id, COUNT(*) AS item_count
    FROM wishlist_items
    GROUP BY wishlist_id
) wi ON wi.wishlist_id = w.id;

--rollback DROP VIEW IF EXISTS v_wishlist_summary;

--changeset fynza:034-v-store-summary-order-count dbms:postgresql
-- Update v_store_summary to replace the hardcoded order_count = 0
-- placeholder with a live count from seller_orders.
CREATE OR REPLACE VIEW v_store_summary AS
SELECT
    st.id,
    st.public_id,
    st.seller_id,
    st.store_name,
    st.slug,
    st.description,
    st.logo_media_id,
    st.banner_media_id,
    st.status,
    st.visibility,
    st.is_active,
    st.business_email,
    st.business_phone,
    st.website,
    st.created_at,
    st.updated_at,
    s.display_name                                                           AS seller_display_name,
    COUNT(DISTINCT p.id) FILTER (WHERE p.is_active = TRUE)::INTEGER          AS product_count,
    0.0                                                                      AS avg_rating,
    0                                                                        AS review_count,
    COUNT(DISTINCT so.id)::INTEGER                                           AS order_count
FROM stores st
JOIN    sellers       s  ON s.id         = st.seller_id
LEFT JOIN products    p  ON p.store_id   = st.id
LEFT JOIN seller_orders so ON so.seller_id = st.seller_id
GROUP BY st.id, st.public_id, st.seller_id, st.store_name, st.slug,
         st.description, st.logo_media_id, st.banner_media_id, st.status,
         st.visibility, st.is_active, st.business_email, st.business_phone,
         st.website, st.created_at, st.updated_at, s.display_name;

--rollback CREATE OR REPLACE VIEW v_store_summary AS SELECT st.id, st.public_id, st.seller_id, st.store_name, st.slug, st.description, st.logo_media_id, st.banner_media_id, st.status, st.visibility, st.is_active, st.business_email, st.business_phone, st.website, st.created_at, st.updated_at, s.display_name AS seller_display_name, COUNT(DISTINCT p.id) FILTER (WHERE p.is_active = TRUE)::INTEGER AS product_count, 0.0 AS avg_rating, 0 AS review_count, 0 AS order_count FROM stores st JOIN sellers s ON s.id = st.seller_id LEFT JOIN products p ON p.store_id = st.id GROUP BY st.id, st.public_id, st.seller_id, st.store_name, st.slug, st.description, st.logo_media_id, st.banner_media_id, st.status, st.visibility, st.is_active, st.business_email, st.business_phone, st.website, st.created_at, st.updated_at, s.display_name;
