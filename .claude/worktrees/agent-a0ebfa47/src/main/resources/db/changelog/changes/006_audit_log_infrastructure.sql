--liquibase formatted sql

--changeset aoms:006-drop-legacy-audit-log
DROP TABLE IF EXISTS audit_log;

--changeset aoms:006-create-audit-logs
CREATE TABLE IF NOT EXISTS audit_logs (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_id       UUID         NOT NULL,
    actor_role     VARCHAR(50)  NOT NULL,
    action         VARCHAR(100) NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      UUID         NOT NULL,
    location_id    UUID,
    previous_state JSONB,
    new_state      JSONB,
    occurred_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    correlation_id UUID
);

CREATE INDEX IF NOT EXISTS idx_audit_actor    ON audit_logs (actor_id,    occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_entity   ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_location ON audit_logs (location_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_action   ON audit_logs (action,      occurred_at DESC);

--changeset aoms:006-create-processed-events
CREATE TABLE IF NOT EXISTS processed_events (
    event_id     UUID        PRIMARY KEY,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

--changeset aoms:006-grant-audit-insert-only
REVOKE ALL ON TABLE audit_logs FROM PUBLIC;
GRANT INSERT ON TABLE audit_logs TO current_user;
