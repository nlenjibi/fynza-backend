--liquibase formatted sql

--changeset fynza:004-mfa-columns
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS mfa_enabled  BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS mfa_secret   VARCHAR(64);

--rollback ALTER TABLE users DROP COLUMN IF EXISTS mfa_enabled, DROP COLUMN IF EXISTS mfa_secret;
