--liquibase formatted sql

--changeset fynza:057-return-exchanges splitStatements:false
CREATE TABLE IF NOT EXISTS return_exchanges (
    id                          BIGSERIAL PRIMARY KEY,
    public_id                   UUID         NOT NULL UNIQUE,
    return_id                   UUID         NOT NULL UNIQUE,
    original_order_id           UUID         NOT NULL,
    exchange_order_id           UUID,
    status                      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    requested_items_description TEXT,
    notes                       TEXT,
    requested_by                UUID         NOT NULL,
    processed_by                UUID,
    processed_at                TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at                  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    version                     BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_return_exchanges_return_id   ON return_exchanges (return_id);
CREATE INDEX IF NOT EXISTS idx_return_exchanges_status      ON return_exchanges (status);
CREATE INDEX IF NOT EXISTS idx_return_exchanges_created_at  ON return_exchanges (created_at);

--changeset fynza:057-return-fraud-signals splitStatements:false
CREATE TABLE IF NOT EXISTS return_fraud_signals (
    id           BIGSERIAL PRIMARY KEY,
    return_id    UUID         NOT NULL,
    customer_id  UUID         NOT NULL,
    signal_type  VARCHAR(40)  NOT NULL,
    description  VARCHAR(500),
    severity     INT          NOT NULL CHECK (severity BETWEEN 1 AND 10),
    detected_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_fraud_signals_return_id   ON return_fraud_signals (return_id);
CREATE INDEX IF NOT EXISTS idx_return_fraud_signals_customer_id ON return_fraud_signals (customer_id);
CREATE INDEX IF NOT EXISTS idx_return_fraud_signals_created_at  ON return_fraud_signals (created_at);
