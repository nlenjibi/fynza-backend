--liquibase formatted sql

--changeset fynza:062-review-rating-aggregates-table
CREATE TABLE IF NOT EXISTS review_rating_aggregates (
    id             BIGSERIAL       PRIMARY KEY,
    target_type    VARCHAR(30)     NOT NULL,
    target_id      UUID            NOT NULL,
    review_count   INTEGER         NOT NULL DEFAULT 0,
    average_rating NUMERIC(3,2)    NOT NULL DEFAULT 0.00,
    rating_1_count INTEGER         NOT NULL DEFAULT 0,
    rating_2_count INTEGER         NOT NULL DEFAULT 0,
    rating_3_count INTEGER         NOT NULL DEFAULT 0,
    rating_4_count INTEGER         NOT NULL DEFAULT 0,
    rating_5_count INTEGER         NOT NULL DEFAULT 0,
    verified_count INTEGER         NOT NULL DEFAULT 0,
    updated_at     TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_review_rating_aggregates_target UNIQUE (target_type, target_id)
);

CREATE INDEX IF NOT EXISTS idx_review_rating_aggregates_target ON review_rating_aggregates (target_type, target_id);
