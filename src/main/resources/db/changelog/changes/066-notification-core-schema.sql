--liquibase formatted sql

--changeset fynza:066-notification splitStatements:false
CREATE TABLE IF NOT EXISTS notification (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL UNIQUE,
    recipient_id        UUID            NOT NULL,
    seller_id           UUID,
    notification_type   VARCHAR(80)     NOT NULL,
    title               VARCHAR(200)    NOT NULL,
    body                TEXT            NOT NULL,
    deep_link           VARCHAR(500),
    entity_type         VARCHAR(60),
    entity_id           UUID,
    is_read             BOOLEAN         NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_notif_recipient_id ON notification (recipient_id);
CREATE INDEX IF NOT EXISTS idx_notif_public_id    ON notification (public_id);
CREATE INDEX IF NOT EXISTS idx_notif_is_read      ON notification (is_read);
CREATE INDEX IF NOT EXISTS idx_notif_created_at   ON notification (created_at);

--changeset fynza:066-notification-dispatch splitStatements:false
CREATE TABLE IF NOT EXISTS notification_dispatch (
    id                      BIGSERIAL       PRIMARY KEY,
    public_id               UUID            NOT NULL UNIQUE,
    recipient_id            UUID,
    recipient_email         VARCHAR(255),
    notification_event_id   UUID            NOT NULL,
    notification_type       VARCHAR(80)     NOT NULL,
    channel                 VARCHAR(20)     NOT NULL,
    status                  VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    subject                 VARCHAR(300),
    text_body               TEXT,
    provider_message_id     VARCHAR(200),
    provider_name           VARCHAR(50),
    attempt_count           INT             NOT NULL DEFAULT 0,
    scheduled_at            TIMESTAMPTZ,
    sent_at                 TIMESTAMPTZ,
    failure_reason          TEXT,
    source_entity_id        UUID,
    slack_channel_id        VARCHAR(32),
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dispatch_recipient_id ON notification_dispatch (recipient_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_status       ON notification_dispatch (status);
CREATE INDEX IF NOT EXISTS idx_dispatch_event_id     ON notification_dispatch (notification_event_id);
CREATE INDEX IF NOT EXISTS idx_dispatch_scheduled_at ON notification_dispatch (scheduled_at);
