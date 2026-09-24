--liquibase formatted sql

--changeset fynza:059-review-votes-table
CREATE TABLE IF NOT EXISTS review_votes (
    id          BIGSERIAL       PRIMARY KEY,
    public_id   UUID            NOT NULL UNIQUE,
    review_id   BIGINT          NOT NULL,
    customer_id UUID            NOT NULL,
    vote_type   VARCHAR(20)     NOT NULL,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_votes_review_customer UNIQUE (review_id, customer_id)
);

CREATE INDEX IF NOT EXISTS idx_review_votes_review_id   ON review_votes (review_id);
CREATE INDEX IF NOT EXISTS idx_review_votes_customer_id ON review_votes (customer_id);

--changeset fynza:059-review-media-table
CREATE TABLE IF NOT EXISTS review_media (
    id               BIGSERIAL       PRIMARY KEY,
    public_id        UUID            NOT NULL UNIQUE,
    review_id        BIGINT          NOT NULL,
    media_reference  VARCHAR(1000)   NOT NULL,
    media_type       VARCHAR(20)     NOT NULL,
    sort_order       INTEGER         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_review_media_review_id ON review_media (review_id);
