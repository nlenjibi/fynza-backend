--liquibase formatted sql

--changeset fynza:036-payout-accounts
CREATE TABLE payout_accounts (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    seller_id           BIGINT          NOT NULL REFERENCES sellers(id) ON DELETE RESTRICT,
    type                VARCHAR(30)     NOT NULL CHECK (type IN ('BANK_ACCOUNT','MOBILE_MONEY','WALLET')),
    provider            VARCHAR(50)     NOT NULL,
    account_name        VARCHAR(255)    NOT NULL,
    account_number      VARCHAR(50)     NOT NULL,
    account_identifier  VARCHAR(255)    NOT NULL,
    bank_code           VARCHAR(20),
    country             VARCHAR(3)      NOT NULL DEFAULT 'GH',
    currency            VARCHAR(3)      NOT NULL DEFAULT 'GHS',
    is_default          BOOLEAN         NOT NULL DEFAULT FALSE,
    is_verified         BOOLEAN         NOT NULL DEFAULT FALSE,
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING','ACTIVE','SUSPENDED','REMOVED')),
    verified_at         TIMESTAMPTZ,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT now()
);
CREATE INDEX idx_payout_accounts_seller_id  ON payout_accounts(seller_id);
CREATE INDEX idx_payout_accounts_status     ON payout_accounts(status);
CREATE INDEX idx_payout_accounts_is_default ON payout_accounts(seller_id, is_default);
CREATE INDEX idx_payout_accounts_is_active  ON payout_accounts(is_active);
