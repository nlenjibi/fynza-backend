--liquibase formatted sql

--changeset fynza:056-return-refund-schema
CREATE TABLE IF NOT EXISTS return_refunds (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL UNIQUE,
    return_id           UUID            NOT NULL UNIQUE,
    order_id            UUID            NOT NULL,
    payment_id          UUID,
    requested_amount    NUMERIC(19,4)   NOT NULL,
    approved_amount     NUMERIC(19,4),
    currency            VARCHAR(3)      NOT NULL DEFAULT 'GHS',
    reason              VARCHAR(500),
    status              VARCHAR(30)     NOT NULL DEFAULT 'PENDING',
    provider_refund_id  VARCHAR(200),
    provider_reference  VARCHAR(200),
    failure_reason      VARCHAR(500),
    requested_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    approved_at         TIMESTAMPTZ,
    processed_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version             BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_return_refunds_return_id  ON return_refunds (return_id);
CREATE INDEX IF NOT EXISTS idx_return_refunds_order_id   ON return_refunds (order_id);
CREATE INDEX IF NOT EXISTS idx_return_refunds_status     ON return_refunds (status);
CREATE INDEX IF NOT EXISTS idx_return_refunds_created_at ON return_refunds (created_at);

--changeset fynza:056-return-reconciliation-schema
CREATE TABLE IF NOT EXISTS return_reconciliations (
    id                  BIGSERIAL   PRIMARY KEY,
    return_id           UUID        NOT NULL,
    return_status       VARCHAR(30),
    shipment_status     VARCHAR(30),
    refund_status       VARCHAR(30),
    inventory_status    VARCHAR(30),
    result              VARCHAR(30) NOT NULL,
    resolved_by         UUID,
    resolved_at         TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_reconciliations_return_id  ON return_reconciliations (return_id);
CREATE INDEX IF NOT EXISTS idx_return_reconciliations_created_at ON return_reconciliations (created_at);
