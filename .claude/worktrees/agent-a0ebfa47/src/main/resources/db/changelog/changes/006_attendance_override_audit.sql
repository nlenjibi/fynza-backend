--liquibase formatted sql

--changeset aoms:006-attendance-override-audit
ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS overridden_at TIMESTAMPTZ;

ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS original_status VARCHAR(30);

ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS revert_reasons TEXT;

ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;

-- Keep the existing override columns compatible with older databases.
ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS override_by UUID REFERENCES employee(id);

ALTER TABLE attendance_record
    ADD COLUMN IF NOT EXISTS override_reason TEXT;

