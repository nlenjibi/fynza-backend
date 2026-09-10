--liquibase formatted sql
--changeset fynza:017-store-views dbms:postgresql

-- v_store_summary: read model for public store pages and admin listing.
-- product_count, avg_rating, review_count, order_count are placeholders (0)
-- until the product/review/order modules add a store_id FK column.
-- A later changeset will replace this view once those columns exist.
CREATE VIEW v_store_summary AS
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
    s.display_name AS seller_display_name,
    0              AS product_count,
    0.0            AS avg_rating,
    0              AS review_count,
    0              AS order_count
FROM stores st
JOIN sellers s ON s.id = st.seller_id;

--rollback DROP VIEW IF EXISTS v_store_summary;
