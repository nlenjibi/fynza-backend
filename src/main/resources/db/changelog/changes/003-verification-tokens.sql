--liquibase formatted sql

--changeset fynza:003-verification-tokens dbms:postgresql

CREATE TABLE IF NOT EXISTS verification_tokens (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL,
    token      VARCHAR(64) NOT NULL UNIQUE,
    token_type VARCHAR(30) NOT NULL,
    expires_at TIMESTAMP   NOT NULL,
    used_at    TIMESTAMP,
    is_used    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP   NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_verification_token ON verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_verification_user  ON verification_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_type  ON verification_tokens(token_type);
CREATE INDEX IF NOT EXISTS idx_verification_used  ON verification_tokens(is_used);

--rollback DROP TABLE IF EXISTS verification_tokens;
