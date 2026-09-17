--liquibase formatted sql

--changeset fynza:029-drop-old-cart-tables
DROP TABLE IF EXISTS stock_reservations CASCADE;
DROP TABLE IF EXISTS cart_items CASCADE;
DROP TABLE IF EXISTS carts CASCADE;

--changeset fynza:029-carts
CREATE TABLE carts (
    id                  BIGSERIAL PRIMARY KEY,
    public_id           UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    user_id             UUID          REFERENCES users(id) ON DELETE SET NULL,
    cart_token          VARCHAR(64)   UNIQUE,
    status              VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','CHECKOUT','ABANDONED','EXPIRED','CONVERTED','MERGED')),
    is_guest            BOOLEAN       NOT NULL DEFAULT FALSE,
    coupon_code         VARCHAR(50),
    subtotal            NUMERIC(19,4) NOT NULL DEFAULT 0,
    discount_amount     NUMERIC(19,4) NOT NULL DEFAULT 0,
    shipping_total      NUMERIC(19,4) NOT NULL DEFAULT 0,
    tax_total           NUMERIC(19,4) NOT NULL DEFAULT 0,
    grand_total         NUMERIC(19,4) NOT NULL DEFAULT 0,
    expires_at          TIMESTAMPTZ,
    merged_into_cart_id BIGINT        REFERENCES carts(id) ON DELETE SET NULL,
    version             BIGINT        NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_carts_active_user ON carts(user_id) WHERE status = 'ACTIVE';
CREATE INDEX idx_carts_cart_token ON carts(cart_token);
CREATE INDEX idx_carts_status ON carts(status);
CREATE INDEX idx_carts_expires_at ON carts(expires_at);

--changeset fynza:029-cart-items
CREATE TABLE cart_items (
    id                BIGSERIAL PRIMARY KEY,
    public_id         UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    cart_id           BIGINT        NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    product_id        UUID          NOT NULL,
    variant_id        UUID,
    store_id          BIGINT,
    quantity          INT           NOT NULL CHECK (quantity >= 1),
    unit_price        NUMERIC(19,4) NOT NULL,
    line_total        NUMERIC(19,4) NOT NULL,
    price_snapshot_at TIMESTAMPTZ,
    price_changed     BOOLEAN       NOT NULL DEFAULT FALSE,
    max_quantity      INT,
    min_quantity      INT           NOT NULL DEFAULT 1,
    version           BIGINT        NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_cart_items_dedup
    ON cart_items(cart_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::UUID));
CREATE INDEX idx_cart_items_cart ON cart_items(cart_id);
CREATE INDEX idx_cart_items_product ON cart_items(product_id);

--changeset fynza:029-stock-reservations
CREATE TABLE stock_reservations (
    id            BIGSERIAL PRIMARY KEY,
    public_id     UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    cart_id       BIGINT        NOT NULL REFERENCES carts(id) ON DELETE CASCADE,
    cart_item_id  BIGINT        NOT NULL REFERENCES cart_items(id) ON DELETE CASCADE,
    product_id    UUID          NOT NULL,
    variant_id    UUID,
    quantity      INT           NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING'
                      CHECK (status IN ('PENDING','CONFIRMED','FAILED','RELEASED','EXPIRED')),
    retry_count   INT           NOT NULL DEFAULT 0,
    error_message VARCHAR(500),
    expires_at    TIMESTAMPTZ   NOT NULL,
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_stock_res_expires ON stock_reservations(expires_at);
CREATE INDEX idx_stock_res_cart_item ON stock_reservations(cart_item_id);
CREATE INDEX idx_stock_res_cart ON stock_reservations(cart_id);
CREATE INDEX idx_stock_res_status ON stock_reservations(status);

--changeset fynza:029-cart-permissions
INSERT INTO permissions (name, description, resource, action, is_system)
VALUES
    ('cart:read',    'View own cart',        'CART', 'READ',   true),
    ('cart:write',   'Add/update cart items','CART', 'WRITE',  true),
    ('cart:delete',  'Remove cart items',    'CART', 'DELETE', true),
    ('cart:validate','Validate cart prices', 'CART', 'EXECUTE',true)
ON CONFLICT (name) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r, permissions p
WHERE r.name = 'CUSTOMER'
  AND p.name IN ('cart:read','cart:write','cart:delete','cart:validate')
ON CONFLICT DO NOTHING;
