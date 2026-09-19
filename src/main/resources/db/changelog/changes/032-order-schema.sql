--liquibase formatted sql

--changeset fynza:032-order-drop-old-tables
DROP TABLE IF EXISTS order_timeline CASCADE;
DROP TABLE IF EXISTS order_items CASCADE;
DROP TABLE IF EXISTS orders CASCADE;
DROP SEQUENCE IF EXISTS order_number_seq;

--changeset fynza:032-order-sequence
CREATE SEQUENCE order_number_seq START 1 INCREMENT 1;

--changeset fynza:032-orders-table
CREATE TABLE orders (
    id                  BIGSERIAL    PRIMARY KEY,
    public_id           UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    is_active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ,

    order_number        VARCHAR(30)  NOT NULL UNIQUE,
    customer_id         UUID         NOT NULL,
    status              VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    payment_status      VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    payment_method      VARCHAR(30),
    coupon_code         VARCHAR(50),
    customer_notes      TEXT,
    tracking_number     VARCHAR(100),

    subtotal            NUMERIC(12,2) NOT NULL DEFAULT 0,
    tax                 NUMERIC(12,2) NOT NULL DEFAULT 0,
    shipping_cost       NUMERIC(12,2) NOT NULL DEFAULT 0,
    discount            NUMERIC(12,2) NOT NULL DEFAULT 0,
    total_amount        NUMERIC(12,2) NOT NULL DEFAULT 0,

    shipping_name       VARCHAR(200),
    shipping_phone      VARCHAR(30),
    shipping_line1      VARCHAR(255),
    shipping_line2      VARCHAR(255),
    shipping_city       VARCHAR(100),
    shipping_state      VARCHAR(100),
    shipping_postal     VARCHAR(20),
    shipping_country    VARCHAR(100),

    billing_name        VARCHAR(200),
    billing_phone       VARCHAR(30),
    billing_line1       VARCHAR(255),
    billing_line2       VARCHAR(255),
    billing_city        VARCHAR(100),
    billing_state       VARCHAR(100),
    billing_postal      VARCHAR(20),
    billing_country     VARCHAR(100)
);

CREATE INDEX idx_orders_customer_id    ON orders(customer_id);
CREATE INDEX idx_orders_status         ON orders(status);
CREATE INDEX idx_orders_payment_status ON orders(payment_status);
CREATE INDEX idx_orders_created_at     ON orders(created_at DESC);

--changeset fynza:032-seller-orders-table
CREATE TABLE seller_orders (
    id              BIGSERIAL    PRIMARY KEY,
    public_id       UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ,

    order_id        BIGINT       NOT NULL REFERENCES orders(id),
    seller_id       BIGINT       NOT NULL,
    status          VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    subtotal        NUMERIC(12,2) NOT NULL DEFAULT 0,
    tracking_number VARCHAR(100),
    notes           TEXT
);

CREATE INDEX idx_seller_orders_order_id  ON seller_orders(order_id);
CREATE INDEX idx_seller_orders_seller_id ON seller_orders(seller_id);

--changeset fynza:032-order-items-table
CREATE TABLE order_items (
    id                  BIGSERIAL    PRIMARY KEY,
    public_id           UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),

    order_id            BIGINT       NOT NULL REFERENCES orders(id),
    seller_order_id     BIGINT       NOT NULL REFERENCES seller_orders(id),

    product_id          UUID         NOT NULL,
    variant_id          UUID,
    seller_id           BIGINT,

    product_name        VARCHAR(500) NOT NULL,
    product_sku         VARCHAR(100),
    product_image_url   VARCHAR(1000),

    quantity            INT          NOT NULL,
    unit_price          NUMERIC(12,2) NOT NULL,
    subtotal            NUMERIC(12,2) NOT NULL
);

CREATE INDEX idx_order_items_order_id        ON order_items(order_id);
CREATE INDEX idx_order_items_seller_order_id ON order_items(seller_order_id);
CREATE INDEX idx_order_items_product_id      ON order_items(product_id);

--changeset fynza:032-order-timeline-table
CREATE TABLE order_timeline (
    id          BIGSERIAL    PRIMARY KEY,
    public_id   UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    order_id    BIGINT       NOT NULL REFERENCES orders(id),
    status      VARCHAR(30)  NOT NULL,
    message     TEXT,
    changed_by  UUID
);

CREATE INDEX idx_order_timeline_order_id ON order_timeline(order_id);
