--liquibase formatted sql

--changeset fynza:067-notification-template splitStatements:false
CREATE TABLE IF NOT EXISTS notification_template (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL UNIQUE,
    notification_type   VARCHAR(80)     NOT NULL,
    channel             VARCHAR(20)     NOT NULL,
    subject             VARCHAR(300)    NOT NULL,
    body                TEXT            NOT NULL,
    html_body           TEXT,
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    locale              VARCHAR(10)     NOT NULL DEFAULT 'en',
    created_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_notification_template_type_channel UNIQUE (notification_type, channel)
);

CREATE INDEX IF NOT EXISTS idx_notif_tmpl_type_channel ON notification_template (notification_type, channel);
CREATE INDEX IF NOT EXISTS idx_notif_tmpl_active        ON notification_template (active);
