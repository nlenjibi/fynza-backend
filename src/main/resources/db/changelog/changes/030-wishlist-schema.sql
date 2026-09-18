--liquibase formatted sql

--changeset fynza:030-drop-old-wishlist-tables
DROP TABLE IF EXISTS wishlist_items CASCADE;
DROP TABLE IF EXISTS wishlists CASCADE;

--changeset fynza:030-wishlists
CREATE TABLE wishlists (
    id                BIGSERIAL     PRIMARY KEY,
    public_id         UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    customer_id       UUID,
    guest_token_hash  VARCHAR(64),
    name              VARCHAR(100)  NOT NULL DEFAULT 'My Wishlist',
    description       TEXT,
    status            VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE'
                          CHECK (status IN ('ACTIVE', 'ARCHIVED', 'DELETED')),
    visibility        VARCHAR(20)   NOT NULL DEFAULT 'PRIVATE'
                          CHECK (visibility IN ('PRIVATE', 'SHARED', 'PUBLIC')),
    is_default        BOOLEAN       NOT NULL DEFAULT FALSE,
    share_token_hash  VARCHAR(64)   UNIQUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_wishlists_customer_id      ON wishlists(customer_id);
CREATE INDEX idx_wishlists_guest_token_hash ON wishlists(guest_token_hash);
CREATE INDEX idx_wishlists_status           ON wishlists(status);
CREATE INDEX idx_wishlists_visibility       ON wishlists(visibility);

CREATE UNIQUE INDEX idx_wishlists_default_customer
    ON wishlists(customer_id)
    WHERE is_default = TRUE AND customer_id IS NOT NULL;

--changeset fynza:030-wishlist-items
CREATE TABLE wishlist_items (
    id                  BIGSERIAL   PRIMARY KEY,
    public_id           UUID        NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    wishlist_id         BIGINT      NOT NULL REFERENCES wishlists(id) ON DELETE CASCADE,
    product_id          UUID        NOT NULL,
    variant_id          UUID,
    notify_on_price_drop BOOLEAN    NOT NULL DEFAULT FALSE,
    notify_on_restock   BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_wishlist_items_wishlist_id ON wishlist_items(wishlist_id);
CREATE INDEX idx_wishlist_items_product_id  ON wishlist_items(product_id);
CREATE INDEX idx_wishlist_items_variant_id  ON wishlist_items(variant_id);

CREATE UNIQUE INDEX idx_wishlist_items_dedup
    ON wishlist_items(wishlist_id, product_id, COALESCE(variant_id, '00000000-0000-0000-0000-000000000000'::UUID));
