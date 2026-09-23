--liquibase formatted sql

--changeset fynza:061-seller-review-responses-table
CREATE TABLE IF NOT EXISTS seller_review_responses (
    id          BIGSERIAL       PRIMARY KEY,
    public_id   UUID            NOT NULL UNIQUE,
    review_id   BIGINT          NOT NULL UNIQUE,
    seller_id   UUID            NOT NULL,
    body        TEXT            NOT NULL,
    status      VARCHAR(30)     NOT NULL DEFAULT 'ACTIVE',
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    version     BIGINT          NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_seller_review_responses_review_id  ON seller_review_responses (review_id);
CREATE INDEX IF NOT EXISTS idx_seller_review_responses_seller_id  ON seller_review_responses (seller_id);
