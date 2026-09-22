--liquibase formatted sql

--changeset fynza:039-payout-schedules
CREATE TABLE payout_schedules (
    id                BIGSERIAL       PRIMARY KEY,
    public_id         UUID            NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    seller_id         BIGINT          NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    frequency         VARCHAR(20)     NOT NULL CHECK (frequency IN ('DAILY','WEEKLY','BIWEEKLY','MONTHLY')),
    day_of_week       INT             CHECK (day_of_week BETWEEN 1 AND 7),
    day_of_month      INT             CHECK (day_of_month BETWEEN 1 AND 31),
    minimum_amount    NUMERIC(19,4)   NOT NULL DEFAULT 20.0000,
    payout_account_id BIGINT          REFERENCES payout_accounts(id) ON DELETE SET NULL,
    enabled           BOOLEAN         NOT NULL DEFAULT TRUE,
    next_run_at       TIMESTAMPTZ,
    last_run_at       TIMESTAMPTZ,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_payout_schedule_seller UNIQUE (seller_id)
);
CREATE INDEX idx_payout_schedule_seller_id ON payout_schedules(seller_id);
CREATE INDEX idx_payout_schedule_next_run  ON payout_schedules(next_run_at) WHERE enabled = TRUE;

--changeset fynza:039-payout-provider-events
CREATE TABLE payout_provider_events (
    id               BIGSERIAL       PRIMARY KEY,
    provider         VARCHAR(50)     NOT NULL,
    event_id         VARCHAR(255)    NOT NULL,
    event_type       VARCHAR(100)    NOT NULL,
    payout_public_id UUID,
    payload_hash     VARCHAR(64)     NOT NULL,
    processed        BOOLEAN         NOT NULL DEFAULT FALSE,
    processed_at     TIMESTAMPTZ,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT uq_provider_event UNIQUE (provider, event_id)
);
CREATE INDEX idx_ppe_provider_event_id ON payout_provider_events(provider, event_id);
CREATE INDEX idx_ppe_payout_public_id  ON payout_provider_events(payout_public_id) WHERE payout_public_id IS NOT NULL;
CREATE INDEX idx_ppe_processed         ON payout_provider_events(processed);

--changeset fynza:039-payout-fee-rules
CREATE TABLE payout_fee_rules (
    id             BIGSERIAL       PRIMARY KEY,
    public_id      UUID            NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    provider       VARCHAR(50)     NOT NULL,
    payout_type    VARCHAR(30)     NOT NULL,
    currency       VARCHAR(3)      NOT NULL DEFAULT 'GHS',
    flat_fee       NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    percentage_fee NUMERIC(5,4)    NOT NULL DEFAULT 0.0000,
    max_fee        NUMERIC(19,4),
    min_fee        NUMERIC(19,4)   DEFAULT 0.0000,
    is_active      BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX idx_fee_rules_lookup ON payout_fee_rules(provider, payout_type, currency) WHERE is_active = TRUE;
