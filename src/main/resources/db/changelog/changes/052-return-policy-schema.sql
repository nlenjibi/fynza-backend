--liquibase formatted sql

--changeset fynza:052-return-policy-schema
CREATE TABLE IF NOT EXISTS return_policies (
    id                           BIGSERIAL       PRIMARY KEY,
    public_id                    UUID            NOT NULL UNIQUE,
    name                         VARCHAR(100)    NOT NULL,
    scope                        VARCHAR(20)     NOT NULL,
    store_id                     UUID,
    product_id                   UUID,
    category_id                  UUID,
    return_window_days           INTEGER         NOT NULL DEFAULT 14,
    is_returnable                BOOLEAN         NOT NULL DEFAULT TRUE,
    condition_required           BOOLEAN         NOT NULL DEFAULT FALSE,
    eligible_reasons             TEXT,
    return_shipping_responsibility VARCHAR(20)   NOT NULL DEFAULT 'CUSTOMER',
    restocking_fee_percent       NUMERIC(5,2),
    refund_method                VARCHAR(30)     NOT NULL DEFAULT 'ORIGINAL_PAYMENT',
    is_active                    BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at                   TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version                      BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_return_policies_scope      ON return_policies (scope);
CREATE INDEX IF NOT EXISTS idx_return_policies_store_id   ON return_policies (store_id);
CREATE INDEX IF NOT EXISTS idx_return_policies_product_id ON return_policies (product_id);

--changeset fynza:052-return-policy-exclusions-schema
CREATE TABLE IF NOT EXISTS return_policy_exclusions (
    id             BIGSERIAL    PRIMARY KEY,
    policy_id      UUID         NOT NULL,
    exclusion_type VARCHAR(20)  NOT NULL,
    reference_id   UUID         NOT NULL,
    reason         VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_rpe_policy_id    ON return_policy_exclusions (policy_id);
CREATE INDEX IF NOT EXISTS idx_rpe_reference_id ON return_policy_exclusions (reference_id);

--changeset fynza:052-platform-return-policy-seed
INSERT INTO return_policies (public_id, name, scope, return_window_days, is_returnable,
    condition_required, return_shipping_responsibility, refund_method, is_active)
VALUES (
    gen_random_uuid(),
    'Platform Default Policy',
    'PLATFORM',
    14,
    TRUE,
    FALSE,
    'CUSTOMER',
    'ORIGINAL_PAYMENT',
    TRUE
) ON CONFLICT DO NOTHING;
