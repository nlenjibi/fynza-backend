--liquibase formatted sql

--changeset fynza:053-returns-escalation-columns
ALTER TABLE returns ADD COLUMN IF NOT EXISTS is_escalated BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE returns ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ;

--changeset fynza:053-return-evidence-schema
CREATE TABLE IF NOT EXISTS return_evidence (
    id               BIGSERIAL       PRIMARY KEY,
    public_id        UUID            NOT NULL UNIQUE,
    return_id        UUID            NOT NULL,
    return_item_id   UUID,
    media_reference  VARCHAR(1000)   NOT NULL,
    evidence_type    VARCHAR(20)     NOT NULL,
    description      VARCHAR(500),
    uploaded_by      UUID            NOT NULL,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_evidence_return_id      ON return_evidence (return_id);
CREATE INDEX IF NOT EXISTS idx_return_evidence_return_item_id ON return_evidence (return_item_id);
CREATE INDEX IF NOT EXISTS idx_return_evidence_created_at     ON return_evidence (created_at);

--changeset fynza:053-return-audit-schema
CREATE TABLE IF NOT EXISTS return_audit (
    id              BIGSERIAL    PRIMARY KEY,
    return_id       UUID         NOT NULL,
    action          VARCHAR(30)  NOT NULL,
    previous_status VARCHAR(30),
    new_status      VARCHAR(30),
    performed_by    UUID,
    reason          VARCHAR(500),
    metadata        TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_audit_return_id  ON return_audit (return_id);
CREATE INDEX IF NOT EXISTS idx_return_audit_created_at ON return_audit (created_at);
