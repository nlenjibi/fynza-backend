--liquibase formatted sql

--changeset fynza:035-payout-financial-accounts
CREATE SEQUENCE IF NOT EXISTS payout_number_seq START 1 INCREMENT 1;

CREATE TABLE seller_financial_accounts (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    seller_id           BIGINT          NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    pending_balance     NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    available_balance   NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    reserved_balance    NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    total_earned        NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    total_paid_out      NUMERIC(19,4)   NOT NULL DEFAULT 0.0000,
    version             BIGINT          NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    CONSTRAINT chk_sfa_pending_balance   CHECK (pending_balance   >= 0),
    CONSTRAINT chk_sfa_available_balance CHECK (available_balance >= 0),
    CONSTRAINT chk_sfa_reserved_balance  CHECK (reserved_balance  >= 0),
    CONSTRAINT uq_sfa_seller             UNIQUE (seller_id)
);
CREATE INDEX idx_sfa_seller_id ON seller_financial_accounts(seller_id);
CREATE INDEX idx_sfa_public_id  ON seller_financial_accounts(public_id);
