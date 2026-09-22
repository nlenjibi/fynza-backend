--liquibase formatted sql

--changeset fynza:037-payouts
CREATE TABLE payouts (
    id                   BIGSERIAL       PRIMARY KEY,
    public_id            UUID            NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    payout_number        VARCHAR(30)     NOT NULL UNIQUE,
    seller_id            BIGINT          NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    payout_account_id    BIGINT          NOT NULL REFERENCES payout_accounts(id) ON DELETE RESTRICT,
    financial_account_id BIGINT          NOT NULL REFERENCES seller_financial_accounts(id) ON DELETE RESTRICT,
    amount               NUMERIC(19,4)   NOT NULL CHECK (amount > 0),
    fee                  NUMERIC(19,4)   NOT NULL DEFAULT 0.0000 CHECK (fee >= 0),
    net_amount           NUMERIC(19,4)   NOT NULL CHECK (net_amount >= 0),
    currency             VARCHAR(3)      NOT NULL DEFAULT 'GHS',
    status               VARCHAR(30)     NOT NULL DEFAULT 'REQUESTED'
                             CHECK (status IN (
                                 'REQUESTED','PENDING_APPROVAL','APPROVED','PROCESSING',
                                 'COMPLETED','FAILED','CANCELLED','REVERSED','ON_HOLD'
                             )),
    idempotency_key      VARCHAR(128)    NOT NULL UNIQUE,
    provider_reference   VARCHAR(255)    UNIQUE,
    failure_reason       TEXT,
    retry_count          INT             NOT NULL DEFAULT 0 CHECK (retry_count >= 0),
    requested_at         TIMESTAMPTZ     NOT NULL DEFAULT now(),
    approved_at          TIMESTAMPTZ,
    processed_at         TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX idx_payouts_seller_id         ON payouts(seller_id);
CREATE INDEX idx_payouts_status            ON payouts(status);
CREATE INDEX idx_payouts_payout_account_id ON payouts(payout_account_id);
CREATE INDEX idx_payouts_financial_acct    ON payouts(financial_account_id);
CREATE INDEX idx_payouts_requested_at      ON payouts(requested_at DESC);
CREATE INDEX idx_payouts_idempotency_key   ON payouts(idempotency_key);
CREATE INDEX idx_payouts_provider_ref      ON payouts(provider_reference) WHERE provider_reference IS NOT NULL;
