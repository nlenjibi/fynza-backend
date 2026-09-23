--liquibase formatted sql

--changeset fynza:058-drop-reviews-table
DROP TABLE IF EXISTS reviews CASCADE;

--changeset fynza:058-create-reviews-table
CREATE TABLE IF NOT EXISTS reviews (
    id                BIGSERIAL       PRIMARY KEY,
    public_id         UUID            NOT NULL UNIQUE,
    customer_id       UUID            NOT NULL,
    product_id        UUID,
    variant_id        UUID,
    store_id          UUID,
    seller_id         UUID,
    order_id          UUID,
    order_item_id     UUID,
    status            VARCHAR(30)     NOT NULL DEFAULT 'PENDING_MODERATION',
    rating            SMALLINT        NOT NULL CHECK (rating >= 1 AND rating <= 5),
    title             VARCHAR(150),
    body              TEXT,
    verified_purchase BOOLEAN         NOT NULL DEFAULT FALSE,
    edited_at         TIMESTAMPTZ,
    published_at      TIMESTAMPTZ,
    is_active         BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version           BIGINT          NOT NULL DEFAULT 0,
    CONSTRAINT uq_reviews_product_customer_order_item UNIQUE (product_id, customer_id, order_item_id)
);

CREATE INDEX IF NOT EXISTS idx_reviews_customer_id               ON reviews (customer_id);
CREATE INDEX IF NOT EXISTS idx_reviews_product_status_created    ON reviews (product_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_seller_status_created     ON reviews (seller_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_status_created            ON reviews (status, created_at);
