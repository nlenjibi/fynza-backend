--liquibase formatted sql

--changeset fynza:050-returns-schema
CREATE TABLE IF NOT EXISTS returns (
    id              BIGSERIAL       PRIMARY KEY,
    public_id       UUID            NOT NULL UNIQUE,
    return_number   VARCHAR(30)     NOT NULL UNIQUE,
    order_id        UUID            NOT NULL,
    customer_id     UUID            NOT NULL,
    seller_id       UUID,
    store_id        UUID,
    status          VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED',
    reason          VARCHAR(30)     NOT NULL,
    customer_note   TEXT,
    admin_note      TEXT,
    rejection_reason VARCHAR(500),
    return_deadline TIMESTAMPTZ,
    requested_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    approved_at     TIMESTAMPTZ,
    received_at     TIMESTAMPTZ,
    rejected_at     TIMESTAMPTZ,
    resolved_at     TIMESTAMPTZ,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version         BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_returns_order_id    ON returns (order_id);
CREATE INDEX IF NOT EXISTS idx_returns_customer_id ON returns (customer_id);
CREATE INDEX IF NOT EXISTS idx_returns_seller_id   ON returns (seller_id);
CREATE INDEX IF NOT EXISTS idx_returns_status      ON returns (status);
CREATE INDEX IF NOT EXISTS idx_returns_created_at  ON returns (created_at);

--changeset fynza:050-return-items-schema
CREATE TABLE IF NOT EXISTS return_items (
    id            BIGSERIAL    PRIMARY KEY,
    public_id     UUID         NOT NULL UNIQUE,
    return_id     UUID         NOT NULL,
    order_item_id UUID         NOT NULL,
    product_id    UUID,
    variant_id    UUID,
    product_name  VARCHAR(255) NOT NULL,
    quantity      INTEGER      NOT NULL,
    reason        VARCHAR(30)  NOT NULL,
    condition     VARCHAR(20),
    unit_price    NUMERIC(19,4),
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_items_return_id     ON return_items (return_id);
CREATE INDEX IF NOT EXISTS idx_return_items_order_item_id ON return_items (order_item_id);
