--liquibase formatted sql

--changeset fynza:065-review-feed-indexes
CREATE INDEX IF NOT EXISTS idx_reviews_product_status_created  ON reviews (product_id, status, created_at, id);
CREATE INDEX IF NOT EXISTS idx_reviews_store_status_created    ON reviews (store_id, status, created_at, id);
CREATE INDEX IF NOT EXISTS idx_reviews_seller_status_created   ON reviews (seller_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_order_id                ON reviews (order_id);
CREATE INDEX IF NOT EXISTS idx_reviews_order_item_id           ON reviews (order_item_id);
CREATE INDEX IF NOT EXISTS idx_reviews_verified                ON reviews (verified_purchase, created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_customer_created        ON reviews (customer_id, created_at);
CREATE INDEX IF NOT EXISTS idx_review_votes_review             ON review_votes (review_id, customer_id);
CREATE INDEX IF NOT EXISTS idx_review_reports_status           ON review_reports (status, created_at);
