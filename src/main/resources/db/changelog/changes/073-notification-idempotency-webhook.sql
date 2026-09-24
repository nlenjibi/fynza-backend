--liquibase formatted sql

--changeset fynza:073-notification-idempotency-key
ALTER TABLE notification
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(200) UNIQUE;

CREATE INDEX IF NOT EXISTS idx_notif_idempotency_key ON notification (idempotency_key);

--changeset fynza:073-notification-webhook-events splitStatements:false
CREATE TABLE IF NOT EXISTS notification_webhook_events (
    id                BIGSERIAL    PRIMARY KEY,
    provider          VARCHAR(40)  NOT NULL,
    provider_event_id VARCHAR(200) NOT NULL,
    event_type        VARCHAR(60),
    payload_hash      VARCHAR(64),
    status            VARCHAR(20)  NOT NULL DEFAULT 'RECEIVED',
    received_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at      TIMESTAMPTZ,
    CONSTRAINT uq_notif_webhook_provider_event UNIQUE (provider, provider_event_id)
);

CREATE INDEX IF NOT EXISTS idx_notif_webhook_status ON notification_webhook_events (status);
