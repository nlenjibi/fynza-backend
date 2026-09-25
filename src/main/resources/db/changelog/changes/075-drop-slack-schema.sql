--liquibase formatted sql

--changeset fynza:075-drop-slack-schema splitStatements:false
DROP TABLE IF EXISTS slack_channel_mapping;

ALTER TABLE notification_channel_config
    DROP COLUMN IF EXISTS slack_enabled;

ALTER TABLE notification_dispatch
    DROP COLUMN IF EXISTS source_entity_id,
    DROP COLUMN IF EXISTS slack_channel_id;
