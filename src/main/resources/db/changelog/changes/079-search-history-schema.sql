--liquibase formatted sql

--changeset fynza:079-search-history splitStatements:false
CREATE TABLE IF NOT EXISTS search_history (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID         NOT NULL,
    query      VARCHAR(500) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_history_user_id    ON search_history (user_id);
CREATE INDEX IF NOT EXISTS idx_search_history_created_at ON search_history (created_at);

