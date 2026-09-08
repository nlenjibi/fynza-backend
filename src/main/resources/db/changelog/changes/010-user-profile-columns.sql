--liquibase formatted sql

--changeset fynza:007-user-profile-columns dbms:postgresql
-- Add user profile and account lifecycle columns for User Management PRD

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS display_name         VARCHAR(100),
    ADD COLUMN IF NOT EXISTS language             VARCHAR(10)  DEFAULT 'en',
    ADD COLUMN IF NOT EXISTS timezone             VARCHAR(50)  DEFAULT 'UTC',
    ADD COLUMN IF NOT EXISTS currency             VARCHAR(3)   DEFAULT 'GHS',
    ADD COLUMN IF NOT EXISTS suspend_reason       TEXT,
    ADD COLUMN IF NOT EXISTS suspended_by         UUID         REFERENCES users(id),
    ADD COLUMN IF NOT EXISTS suspended_at         TIMESTAMP,
    ADD COLUMN IF NOT EXISTS disabled_at          TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS scheduled_deletion_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_users_deletion_requested ON users(deletion_requested_at)
    WHERE deletion_requested_at IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_users_scheduled_deletion ON users(scheduled_deletion_at)
    WHERE scheduled_deletion_at IS NOT NULL;
