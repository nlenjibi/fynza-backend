--liquibase formatted sql

-- =============================================================================
-- 023 — Pricing Management Schema
-- Module: PRICING-09
-- Milestone: M1 (Foundation) + M3 (History) + M4 partial (Tiers)
-- =============================================================================

--changeset fynza:023-01 labels:pricing dbms:postgresql
CREATE TABLE IF NOT EXISTS price_lists (
    id          BIGSERIAL    PRIMARY KEY,
    public_id   UUID         NOT NULL UNIQUE,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_price_lists_is_default ON price_lists(is_default);
CREATE INDEX IF NOT EXISTS idx_price_lists_is_active  ON price_lists(is_active);

-- Seed the default price list
INSERT INTO price_lists (public_id, name, description, is_default, is_active)
VALUES (gen_random_uuid(), 'Default', 'Default Fynza price list', TRUE, TRUE)
ON CONFLICT DO NOTHING;

--changeset fynza:023-02 labels:pricing dbms:postgresql
-- Core price record.
-- product_id references products.id (UUID PK).
-- variant_id references product_variants.id (UUID PK), nullable = product-level price.
-- No hard FK on product_id / variant_id to keep the pricing module loosely coupled.
CREATE TABLE IF NOT EXISTS prices (
    id             BIGSERIAL      PRIMARY KEY,
    public_id      UUID           NOT NULL UNIQUE,
    price_list_id  BIGINT         NOT NULL REFERENCES price_lists(id),
    product_id     UUID           NOT NULL,
    variant_id     UUID,
    amount         DECIMAL(19, 4) NOT NULL,
    sale_amount    DECIMAL(19, 4),
    currency       VARCHAR(3)     NOT NULL DEFAULT 'GHS',
    status         VARCHAR(20)    NOT NULL DEFAULT 'DRAFT',
    valid_from     TIMESTAMPTZ,
    valid_until    TIMESTAMPTZ,
    version        BIGINT         NOT NULL DEFAULT 0,
    created_by     UUID           NOT NULL,
    is_active      BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_price_amount_non_negative     CHECK (amount     >= 0),
    CONSTRAINT chk_price_sale_amount_non_negative CHECK (sale_amount IS NULL OR sale_amount >= 0),
    CONSTRAINT chk_price_valid_until_after_from  CHECK (valid_until IS NULL OR valid_from IS NULL OR valid_until > valid_from),
    CONSTRAINT chk_price_status CHECK (status IN ('DRAFT','ACTIVE','SCHEDULED','EXPIRED','DISABLED')),
    CONSTRAINT chk_price_currency CHECK (currency IN ('GHS','USD','EUR','GBP','NGN'))
);

CREATE INDEX IF NOT EXISTS idx_prices_product_id     ON prices(product_id);
CREATE INDEX IF NOT EXISTS idx_prices_variant_id     ON prices(variant_id);
CREATE INDEX IF NOT EXISTS idx_prices_price_list_id  ON prices(price_list_id);
CREATE INDEX IF NOT EXISTS idx_prices_status         ON prices(status);
CREATE INDEX IF NOT EXISTS idx_prices_currency       ON prices(currency);
CREATE INDEX IF NOT EXISTS idx_prices_valid_from     ON prices(valid_from);
CREATE INDEX IF NOT EXISTS idx_prices_valid_until    ON prices(valid_until);
CREATE INDEX IF NOT EXISTS idx_prices_created_by     ON prices(created_by);
CREATE INDEX IF NOT EXISTS idx_prices_is_active      ON prices(is_active);

-- Prevent two ACTIVE prices for the same product+variant+currency combo in the same price list.
-- This is a partial unique index enforced only when status = 'ACTIVE'.
CREATE UNIQUE INDEX IF NOT EXISTS uidx_prices_active_unique
    ON prices(price_list_id, product_id, currency)
    WHERE status = 'ACTIVE' AND variant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uidx_prices_variant_active_unique
    ON prices(price_list_id, product_id, variant_id, currency)
    WHERE status = 'ACTIVE' AND variant_id IS NOT NULL;

--changeset fynza:023-03 labels:pricing dbms:postgresql
CREATE TABLE IF NOT EXISTS price_tiers (
    id            BIGSERIAL      PRIMARY KEY,
    public_id     UUID           NOT NULL UNIQUE,
    price_id      BIGINT         NOT NULL REFERENCES prices(id) ON DELETE CASCADE,
    min_quantity  INTEGER        NOT NULL,
    max_quantity  INTEGER,
    unit_price    DECIMAL(19, 4) NOT NULL,
    currency      VARCHAR(3)     NOT NULL DEFAULT 'GHS',
    is_active     BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT chk_tier_min_quantity_positive CHECK (min_quantity > 0),
    CONSTRAINT chk_tier_max_gte_min           CHECK (max_quantity IS NULL OR max_quantity >= min_quantity),
    CONSTRAINT chk_tier_unit_price_non_neg    CHECK (unit_price >= 0)
);

CREATE INDEX IF NOT EXISTS idx_price_tiers_price_id ON price_tiers(price_id);

--changeset fynza:023-04 labels:pricing dbms:postgresql
-- Immutable audit trail. No updated_at.
CREATE TABLE IF NOT EXISTS price_history (
    id           BIGSERIAL      PRIMARY KEY,
    price_id     BIGINT         NOT NULL REFERENCES prices(id),
    old_amount   DECIMAL(19, 4),
    new_amount   DECIMAL(19, 4) NOT NULL,
    old_currency VARCHAR(3),
    new_currency VARCHAR(3)     NOT NULL,
    old_status   VARCHAR(20),
    new_status   VARCHAR(20)    NOT NULL,
    changed_by   UUID           NOT NULL,
    reason       VARCHAR(500),
    created_at   TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_price_history_price_id   ON price_history(price_id);
CREATE INDEX IF NOT EXISTS idx_price_history_created_at ON price_history(created_at);

--changeset fynza:023-05 labels:pricing dbms:postgresql
CREATE TABLE IF NOT EXISTS price_overrides (
    id          BIGSERIAL      PRIMARY KEY,
    public_id   UUID           NOT NULL UNIQUE,
    price_id    BIGINT         NOT NULL REFERENCES prices(id),
    reason      TEXT           NOT NULL,
    old_amount  DECIMAL(19, 4) NOT NULL,
    new_amount  DECIMAL(19, 4) NOT NULL,
    approved_by UUID           NOT NULL,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_price_overrides_price_id ON price_overrides(price_id);
