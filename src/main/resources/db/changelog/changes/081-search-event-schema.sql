--liquibase formatted sql

--changeset fynza:081-search-event splitStatements:false
CREATE TABLE IF NOT EXISTS search_event (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID,
    session_id          VARCHAR(100),
    query               VARCHAR(500) NOT NULL,
    normalized_query    VARCHAR(500),
    result_count        INT          NOT NULL DEFAULT 0,
    sort_by             VARCHAR(50),
    selected_product_id UUID,
    event_type          VARCHAR(50)  NOT NULL,
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_search_event_user_id    ON search_event (user_id);
CREATE INDEX IF NOT EXISTS idx_search_event_created_at ON search_event (created_at);
CREATE INDEX IF NOT EXISTS idx_search_event_type       ON search_event (event_type);

