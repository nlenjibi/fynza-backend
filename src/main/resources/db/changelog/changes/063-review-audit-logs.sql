--liquibase formatted sql

--changeset fynza:063-review-audit-logs-table
CREATE TABLE IF NOT EXISTS review_audit_logs (
    id         BIGSERIAL       PRIMARY KEY,
    review_id  BIGINT          NOT NULL,
    actor_id   UUID            NOT NULL,
    action     VARCHAR(50)     NOT NULL,
    old_value  TEXT,
    new_value  TEXT,
    reason     TEXT,
    metadata   TEXT,
    created_at TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_audit_logs_review_id ON review_audit_logs (review_id);
CREATE INDEX IF NOT EXISTS idx_review_audit_logs_actor_id  ON review_audit_logs (actor_id);
