--liquibase formatted sql

--changeset fynza:078-search-analytics splitStatements:false
CREATE TABLE IF NOT EXISTS search_analytics (
    id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    search_query         VARCHAR(500) NOT NULL,
    search_date          DATE         NOT NULL,
    search_count         INT          NOT NULL DEFAULT 1,
    result_count         INT          NOT NULL DEFAULT 0,
    click_count          INT          NOT NULL DEFAULT 0,
    is_zero_results      BOOLEAN      NOT NULL DEFAULT FALSE,
    search_type          VARCHAR(50),
    user_id              UUID,
    session_id           VARCHAR(100),
    ip_address           VARCHAR(50),
    avg_response_time_ms BIGINT,
    is_active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (search_query, search_date)
);

CREATE INDEX IF NOT EXISTS idx_search_analytics_query ON search_analytics (search_query);
CREATE INDEX IF NOT EXISTS idx_search_analytics_date  ON search_analytics (search_date);

