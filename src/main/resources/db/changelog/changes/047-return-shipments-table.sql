--liquibase formatted sql

--changeset fynza:047-return-shipments-table splitStatements:false
CREATE TABLE IF NOT EXISTS return_shipments (
    id                   BIGSERIAL PRIMARY KEY,
    public_id            UUID          NOT NULL UNIQUE,
    return_id            UUID,
    original_shipment_id UUID          NOT NULL REFERENCES shipments(public_id),
    carrier_id           UUID,
    tracking_number      VARCHAR(100),
    status               VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    notes                TEXT,
    is_active            BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMP     NOT NULL,
    updated_at           TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_return_shipments_original ON return_shipments(original_shipment_id);
--rollback DROP TABLE IF EXISTS return_shipments CASCADE;
