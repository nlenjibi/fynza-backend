--liquibase formatted sql

--changeset fynza:048-shipping-reconciliation-table
CREATE TABLE IF NOT EXISTS shipping_reconciliation (
    id                      BIGSERIAL PRIMARY KEY,
    public_id               UUID          NOT NULL UNIQUE,
    shipment_id             UUID          NOT NULL,
    carrier                 VARCHAR(50),
    carrier_tracking_number VARCHAR(200),
    reconciliation_date     DATE          NOT NULL DEFAULT CURRENT_DATE,
    expected_status         VARCHAR(30),
    actual_status           VARCHAR(30),
    discrepancy_type        VARCHAR(50),
    discrepancy_notes       TEXT,
    resolved                BOOLEAN       NOT NULL DEFAULT FALSE,
    resolved_at             TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipping_reconciliation_shipment_id
    ON shipping_reconciliation (shipment_id);

CREATE INDEX IF NOT EXISTS idx_shipping_reconciliation_date
    ON shipping_reconciliation (reconciliation_date);

CREATE INDEX IF NOT EXISTS idx_shipping_reconciliation_resolved
    ON shipping_reconciliation (resolved);
