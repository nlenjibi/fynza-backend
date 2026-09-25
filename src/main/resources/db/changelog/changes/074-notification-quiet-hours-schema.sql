--liquibase formatted sql

--changeset fynza:074-notification-quiet-hours-schema splitStatements:false
CREATE TABLE IF NOT EXISTS notification_quiet_hours (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     UUID         NOT NULL UNIQUE,
    start_time  TIME         NOT NULL,
    end_time    TIME         NOT NULL,
    timezone    VARCHAR(50)  NOT NULL DEFAULT 'UTC',
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notif_quiet_user ON notification_quiet_hours (user_id);
