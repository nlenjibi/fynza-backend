--liquibase formatted sql

--changeset fynza:046-shipment-exceptions-table splitStatements:false
CREATE TABLE IF NOT EXISTS shipment_exceptions (
    id               BIGSERIAL PRIMARY KEY,
    public_id        UUID          NOT NULL UNIQUE,
    shipment_id      UUID          NOT NULL REFERENCES shipments(public_id),
    type             VARCHAR(30)   NOT NULL,
    severity         VARCHAR(20)   NOT NULL DEFAULT 'MEDIUM',
    description      TEXT,
    status           VARCHAR(20)   NOT NULL DEFAULT 'OPEN',
    detected_at      TIMESTAMP     NOT NULL,
    resolved_at      TIMESTAMP,
    resolved_by      VARCHAR(100),
    resolution_notes TEXT,
    is_active        BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP     NOT NULL,
    updated_at       TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_shipment_exceptions_shipment_id ON shipment_exceptions(shipment_id);
CREATE INDEX IF NOT EXISTS idx_shipment_exceptions_status      ON shipment_exceptions(status);
--rollback DROP TABLE IF EXISTS shipment_exceptions CASCADE;
