--liquibase formatted sql

--changeset fynza:080-search-synonym splitStatements:false
CREATE TABLE IF NOT EXISTS search_synonym (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    term       VARCHAR(200) NOT NULL UNIQUE,
    synonyms   VARCHAR(1000) NOT NULL DEFAULT '',
    locale     VARCHAR(10)  NOT NULL DEFAULT 'en',
    status     VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_synonym_term   ON search_synonym (term);
CREATE INDEX IF NOT EXISTS idx_synonym_status ON search_synonym (status);

