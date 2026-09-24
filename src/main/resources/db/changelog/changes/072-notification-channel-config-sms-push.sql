--liquibase formatted sql

--changeset fynza:072-notification-channel-config-sms-push
ALTER TABLE notification_channel_config
    ADD COLUMN IF NOT EXISTS sms_enabled  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS push_enabled BOOLEAN NOT NULL DEFAULT FALSE;
