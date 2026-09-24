--liquibase formatted sql

--changeset fynza:069-slack-channel-mapping splitStatements:false
CREATE TABLE IF NOT EXISTS slack_channel_mapping (
    id               BIGSERIAL    PRIMARY KEY,
    public_id        UUID         NOT NULL UNIQUE,
    seller_id        UUID,
    slack_channel_id VARCHAR(32)  NOT NULL,
    label            VARCHAR(255),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_slack_mapping_seller_id ON slack_channel_mapping (seller_id);
CREATE INDEX IF NOT EXISTS idx_slack_mapping_active     ON slack_channel_mapping (active);
