-- liquibase formatted sql

-- changeset fynza:006-seller-notification-settings
CREATE TABLE IF NOT EXISTS seller_notification_settings (
    id                BIGSERIAL PRIMARY KEY,
    seller_id         BIGINT NOT NULL UNIQUE REFERENCES seller_profiles(id) ON DELETE CASCADE,
    new_orders        BOOLEAN NOT NULL DEFAULT TRUE,
    order_updates     BOOLEAN NOT NULL DEFAULT TRUE,
    customer_messages BOOLEAN NOT NULL DEFAULT TRUE,
    stock_alerts      BOOLEAN NOT NULL DEFAULT TRUE,
    payment_updates   BOOLEAN NOT NULL DEFAULT TRUE,
    refund_requests   BOOLEAN NOT NULL DEFAULT TRUE,
    promotional_emails BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at        TIMESTAMP
);
