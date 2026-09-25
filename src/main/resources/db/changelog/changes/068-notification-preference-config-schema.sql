--liquibase formatted sql

--changeset fynza:068-notification-preference splitStatements:false
CREATE TABLE IF NOT EXISTS notification_preference (
    id                  BIGSERIAL   PRIMARY KEY,
    public_id           UUID        NOT NULL UNIQUE,
    user_id             UUID        NOT NULL,
    notification_type   VARCHAR(80) NOT NULL,
    channel             VARCHAR(20) NOT NULL,
    enabled             BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notif_pref_user_type_channel UNIQUE (user_id, notification_type, channel)
);

CREATE INDEX IF NOT EXISTS idx_notif_pref_user_id ON notification_preference (user_id);

--changeset fynza:068-notification-channel-config splitStatements:false
CREATE TABLE IF NOT EXISTS notification_channel_config (
    id                   BIGSERIAL   PRIMARY KEY,
    public_id            UUID        NOT NULL UNIQUE,
    notification_type    VARCHAR(80) NOT NULL UNIQUE,
    email_enabled        BOOLEAN     NOT NULL DEFAULT TRUE,
    in_app_enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    slack_enabled        BOOLEAN     NOT NULL DEFAULT FALSE,
    max_retries          INT         NOT NULL DEFAULT 3,
    retry_delay_seconds  INT         NOT NULL DEFAULT 60,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
