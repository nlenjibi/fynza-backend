--liquibase formatted sql

--changeset fynza:060-review-reports-table
CREATE TABLE IF NOT EXISTS review_reports (
    id          BIGSERIAL       PRIMARY KEY,
    public_id   UUID            NOT NULL UNIQUE,
    review_id   BIGINT          NOT NULL,
    reporter_id UUID            NOT NULL,
    reason      VARCHAR(50)     NOT NULL,
    description TEXT,
    status      VARCHAR(30)     NOT NULL DEFAULT 'OPEN',
    resolved_by UUID,
    resolved_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_reports_review_reporter UNIQUE (review_id, reporter_id)
);

CREATE INDEX IF NOT EXISTS idx_review_reports_review_id      ON review_reports (review_id);
CREATE INDEX IF NOT EXISTS idx_review_reports_status_created ON review_reports (status, created_at);

--changeset fynza:060-review-moderation-log-table
CREATE TABLE IF NOT EXISTS review_moderation_log (
    id           BIGSERIAL       PRIMARY KEY,
    review_id    BIGINT          NOT NULL,
    action       VARCHAR(30)     NOT NULL,
    reason_code  VARCHAR(100),
    moderator_id UUID,
    notes        TEXT,
    created_at   TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_moderation_log_review_id ON review_moderation_log (review_id);
