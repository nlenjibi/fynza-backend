--liquibase formatted sql

--changeset fynza:045-delivery-attempts-table splitStatements:false
CREATE TABLE IF NOT EXISTS delivery_attempts (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID         NOT NULL UNIQUE,
    shipment_id     UUID         NOT NULL REFERENCES shipments(public_id),
    attempt_number  INT          NOT NULL DEFAULT 1,
    status          VARCHAR(30)  NOT NULL DEFAULT 'FAILED',
    failure_reason  VARCHAR(100),
    location        VARCHAR(255),
    notes           TEXT,
    next_attempt_at TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL,
    updated_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_delivery_attempts_shipment_id ON delivery_attempts(shipment_id);
--rollback DROP TABLE IF EXISTS delivery_attempts CASCADE;
