--liquibase formatted sql

--changeset fynza:038-ledger-entries
CREATE TABLE ledger_entries (
    id                   BIGSERIAL       PRIMARY KEY,
    public_id            UUID            NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    financial_account_id BIGINT          NOT NULL REFERENCES seller_financial_accounts(id) ON DELETE RESTRICT,
    entry_type           VARCHAR(50)     NOT NULL
                             CHECK (entry_type IN (
                                 'ORDER_EARNING','PAYOUT_RESERVATION','PAYOUT_COMPLETED',
                                 'PAYOUT_FEE','REFUND','CHARGEBACK','PAYOUT_REVERSAL','PAYOUT_RELEASE'
                             )),
    direction            VARCHAR(6)      NOT NULL CHECK (direction IN ('CREDIT','DEBIT')),
    reference_type       VARCHAR(50),
    reference_id         UUID,
    amount               NUMERIC(19,4)   NOT NULL CHECK (amount > 0),
    currency             VARCHAR(3)      NOT NULL DEFAULT 'GHS',
    balance_before       NUMERIC(19,4)   NOT NULL,
    balance_after        NUMERIC(19,4)   NOT NULL,
    description          TEXT,
    created_at           TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX idx_ledger_financial_account ON ledger_entries(financial_account_id);
CREATE INDEX idx_ledger_entry_type        ON ledger_entries(entry_type);
CREATE INDEX idx_ledger_reference_id      ON ledger_entries(reference_id) WHERE reference_id IS NOT NULL;
CREATE INDEX idx_ledger_created_at        ON ledger_entries(created_at DESC);
