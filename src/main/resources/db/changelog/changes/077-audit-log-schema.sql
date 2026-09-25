--liquibase formatted sql

--changeset fynza:077-audit-log-schema splitStatements:false
CREATE TABLE IF NOT EXISTS audit_log (
    id               UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    actor_public_id  UUID         NOT NULL,
    actor_email      VARCHAR(255),
    actor_role       VARCHAR(30)  NOT NULL,
    action           VARCHAR(100) NOT NULL,
    entity_type      VARCHAR(100) NOT NULL,
    entity_public_id UUID,
    previous_state   JSONB,
    new_state        JSONB,
    reason           TEXT,
    ip_address       VARCHAR(45),
    occurred_at      TIMESTAMPTZ  NOT NULL,
    correlation_id   UUID,
    status           VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_audit_actor       ON audit_log (actor_public_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity      ON audit_log (entity_type, entity_public_id);
CREATE INDEX IF NOT EXISTS idx_audit_action      ON audit_log (action);
CREATE INDEX IF NOT EXISTS idx_audit_occurred_at ON audit_log (occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_status      ON audit_log (status);
