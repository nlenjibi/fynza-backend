--liquibase formatted sql

--changeset fynza:071-notification-devices splitStatements:false
CREATE TABLE IF NOT EXISTS notification_devices (
    id            BIGSERIAL    PRIMARY KEY,
    public_id     UUID         NOT NULL UNIQUE,
    user_id       UUID         NOT NULL,
    platform      VARCHAR(10)  NOT NULL,
    device_token  VARCHAR(500) NOT NULL UNIQUE,
    app_version   VARCHAR(30),
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    last_seen_at  TIMESTAMPTZ,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notif_device_user_id ON notification_devices (user_id);
CREATE INDEX IF NOT EXISTS idx_notif_device_status  ON notification_devices (status);
