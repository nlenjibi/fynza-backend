--liquibase formatted sql
--changeset fynza:019-product-extensions dbms:postgresql

-- Drop legacy product_images table (replaced by product_media)
DROP TABLE IF EXISTS product_images;

-- Product media: references to the Storage/Media module
CREATE TABLE IF NOT EXISTS product_media (
    id         BIGSERIAL    PRIMARY KEY,
    product_id UUID         NOT NULL REFERENCES products(id),
    media_id   VARCHAR(255) NOT NULL,
    type       VARCHAR(30)  NOT NULL DEFAULT 'IMAGE',
    sort_order INTEGER      NOT NULL DEFAULT 0,
    is_primary BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_product_media_product_id ON product_media(product_id);
CREATE INDEX IF NOT EXISTS idx_product_media_primary    ON product_media(product_id, is_primary);

-- Product categories: many-to-many, one primary category per product
CREATE TABLE IF NOT EXISTS product_categories (
    id          BIGSERIAL PRIMARY KEY,
    product_id  UUID      NOT NULL REFERENCES products(id),
    category_id UUID      NOT NULL REFERENCES categories(id),
    is_primary  BOOLEAN   NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_product_category UNIQUE (product_id, category_id)
);
CREATE INDEX IF NOT EXISTS idx_product_categories_product  ON product_categories(product_id);
CREATE INDEX IF NOT EXISTS idx_product_categories_category ON product_categories(category_id);

-- Product attributes: flexible key-value pairs per product
CREATE TABLE IF NOT EXISTS product_attributes (
    id              BIGSERIAL    PRIMARY KEY,
    product_id      UUID         NOT NULL REFERENCES products(id),
    attribute_key   VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(500) NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_product_attrs_product ON product_attributes(product_id);

-- Product variant attributes: key-value pairs per variant
CREATE TABLE IF NOT EXISTS product_variant_attributes (
    id              BIGSERIAL    PRIMARY KEY,
    variant_id      UUID         NOT NULL REFERENCES product_variants(id),
    attribute_key   VARCHAR(100) NOT NULL,
    attribute_value VARCHAR(500) NOT NULL,
    sort_order      INTEGER      NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_variant_attrs_variant ON product_variant_attributes(variant_id);

-- Product status history: audit trail of lifecycle transitions
CREATE TABLE IF NOT EXISTS product_status_history (
    id              BIGSERIAL   PRIMARY KEY,
    product_id      UUID        NOT NULL REFERENCES products(id),
    previous_status VARCHAR(50),
    new_status      VARCHAR(50) NOT NULL,
    reason          TEXT,
    changed_by      UUID,
    created_at      TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_product_status_hist_product ON product_status_history(product_id);
CREATE INDEX IF NOT EXISTS idx_product_status_hist_created ON product_status_history(created_at);

-- Update v_store_summary now that products.store_id exists
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
    s.display_name                                              AS seller_display_name,
    COUNT(DISTINCT p.id) FILTER (WHERE p.is_active = TRUE)::INTEGER AS product_count,
    0.0                                                         AS avg_rating,
    0                                                           AS review_count,
    0                                                           AS order_count
FROM stores st
JOIN sellers s ON s.id = st.seller_id
LEFT JOIN products p ON p.store_id = st.id
GROUP BY st.id, st.public_id, st.seller_id, st.store_name, st.slug, st.description,
         st.logo_media_id, st.banner_media_id, st.status, st.visibility, st.is_active,
         st.business_email, st.business_phone, st.website, st.created_at, st.updated_at,
         s.display_name;

--rollback DROP VIEW IF EXISTS v_store_summary;
--rollback CREATE VIEW v_store_summary AS SELECT st.id, st.public_id, st.seller_id, st.store_name, st.slug, st.description, st.logo_media_id, st.banner_media_id, st.status, st.visibility, st.is_active, st.business_email, st.business_phone, st.website, st.created_at, st.updated_at, s.display_name AS seller_display_name, 0 AS product_count, 0.0 AS avg_rating, 0 AS review_count, 0 AS order_count FROM stores st JOIN sellers s ON s.id = st.seller_id;
--rollback DROP TABLE IF EXISTS product_status_history;
--rollback DROP TABLE IF EXISTS product_variant_attributes;
--rollback DROP TABLE IF EXISTS product_attributes;
--rollback DROP TABLE IF EXISTS product_categories;
--rollback DROP TABLE IF EXISTS product_media;
