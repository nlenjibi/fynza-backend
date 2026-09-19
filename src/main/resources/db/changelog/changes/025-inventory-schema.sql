--liquibase formatted sql

-- =============================================================================
-- 025 — Inventory Management Schema
-- Module: INVENTORY-10
-- Milestone: M1 (Foundation) + M2 (Reservations) + M3 (Stock Ledger) + M4 (Multi-Location)
-- =============================================================================

--changeset fynza:025-01 labels:inventory dbms:postgresql
CREATE TABLE IF NOT EXISTS inventory_locations (
    id            BIGSERIAL    PRIMARY KEY,
    public_id     UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    seller_id     BIGINT       NOT NULL,
    store_id      BIGINT,
    name          VARCHAR(150) NOT NULL,
    code          VARCHAR(50)  NOT NULL,
    location_type VARCHAR(30)  NOT NULL DEFAULT 'WAREHOUSE',
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_inventory_location_type   CHECK (location_type IN ('WAREHOUSE','STORE','FULFILLMENT_CENTER','DROPSHIPPER','SUPPLIER','VIRTUAL')),
    CONSTRAINT chk_inventory_location_status CHECK (status IN ('ACTIVE','INACTIVE','CLOSED'))
);

CREATE INDEX IF NOT EXISTS idx_inv_locations_seller_id ON inventory_locations(seller_id);
CREATE INDEX IF NOT EXISTS idx_inv_locations_store_id  ON inventory_locations(store_id);
CREATE INDEX IF NOT EXISTS idx_inv_locations_status    ON inventory_locations(status);
CREATE UNIQUE INDEX IF NOT EXISTS uidx_inv_locations_seller_code ON inventory_locations(seller_id, code);

--changeset fynza:025-02 labels:inventory dbms:postgresql
-- Core inventory record per (product, variant, location).
-- product_id/variant_id: no hard FK — loosely coupled to product module.
CREATE TABLE IF NOT EXISTS inventory (
    id                   BIGSERIAL   PRIMARY KEY,
    public_id            UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    product_id           UUID        NOT NULL,
    variant_id           UUID,
    seller_id            BIGINT      NOT NULL,
    store_id             BIGINT,
    location_id          BIGINT      NOT NULL REFERENCES inventory_locations(id),
    on_hand_quantity     INT         NOT NULL DEFAULT 0,
    reserved_quantity    INT         NOT NULL DEFAULT 0,
    incoming_quantity    INT         NOT NULL DEFAULT 0,
    damaged_quantity     INT         NOT NULL DEFAULT 0,
    low_stock_threshold  INT         NOT NULL DEFAULT 5,
    allow_backorder      BOOLEAN     NOT NULL DEFAULT FALSE,
    version              BIGINT      NOT NULL DEFAULT 0,
    is_active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_inv_on_hand_non_negative   CHECK (on_hand_quantity  >= 0),
    CONSTRAINT chk_inv_reserved_non_negative  CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inv_incoming_non_negative  CHECK (incoming_quantity >= 0),
    CONSTRAINT chk_inv_damaged_non_negative   CHECK (damaged_quantity  >= 0),
    CONSTRAINT chk_inv_threshold_non_negative CHECK (low_stock_threshold >= 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_product_id   ON inventory(product_id);
CREATE INDEX IF NOT EXISTS idx_inventory_variant_id   ON inventory(variant_id);
CREATE INDEX IF NOT EXISTS idx_inventory_seller_id    ON inventory(seller_id);
CREATE INDEX IF NOT EXISTS idx_inventory_location_id  ON inventory(location_id);
CREATE INDEX IF NOT EXISTS idx_inventory_is_active    ON inventory(is_active);

-- One record per (product, variant, location) — NULL variant_id treated as product-level.
CREATE UNIQUE INDEX IF NOT EXISTS uidx_inventory_product_location
    ON inventory(product_id, location_id) WHERE variant_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uidx_inventory_variant_location
    ON inventory(product_id, variant_id, location_id) WHERE variant_id IS NOT NULL;

--changeset fynza:025-03 labels:inventory dbms:postgresql
CREATE TABLE IF NOT EXISTS inventory_reservations (
    id           BIGSERIAL   PRIMARY KEY,
    public_id    UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    inventory_id BIGINT      NOT NULL REFERENCES inventory(id),
    order_id     UUID,
    quantity     INT         NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    reserved_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    expires_at   TIMESTAMPTZ,
    released_at  TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_inv_reservation_quantity_pos CHECK (quantity > 0),
    CONSTRAINT chk_inv_reservation_status CHECK (status IN ('ACTIVE','CONFIRMED','RELEASED','EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_inv_reservations_inventory_status ON inventory_reservations(inventory_id, status);
CREATE INDEX IF NOT EXISTS idx_inv_reservations_expires_at       ON inventory_reservations(expires_at);
CREATE INDEX IF NOT EXISTS idx_inv_reservations_order_id         ON inventory_reservations(order_id);

--changeset fynza:025-04 labels:inventory dbms:postgresql
-- Immutable append-only ledger — no updated_at.
CREATE TABLE IF NOT EXISTS stock_movements (
    id                BIGSERIAL   PRIMARY KEY,
    inventory_id      BIGINT      NOT NULL REFERENCES inventory(id),
    movement_type     VARCHAR(30) NOT NULL,
    quantity          INT         NOT NULL,
    previous_quantity INT         NOT NULL DEFAULT 0,
    new_quantity      INT         NOT NULL DEFAULT 0,
    reference_type    VARCHAR(50),
    reference_id      UUID,
    reason            TEXT,
    performed_by      UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_stock_movement_type CHECK (movement_type IN ('PURCHASE','RECEIPT','SALE','RESERVATION','RELEASE','RETURN','ADJUSTMENT','DAMAGE','LOSS','TRANSFER_IN','TRANSFER_OUT'))
);

CREATE INDEX IF NOT EXISTS idx_stock_movements_inventory_id  ON stock_movements(inventory_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_movement_type ON stock_movements(movement_type);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at    ON stock_movements(created_at);

--changeset fynza:025-05 labels:inventory dbms:postgresql
CREATE TABLE IF NOT EXISTS inventory_transfers (
    id                      BIGSERIAL   PRIMARY KEY,
    public_id               UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    source_location_id      BIGINT      NOT NULL REFERENCES inventory_locations(id),
    destination_location_id BIGINT      NOT NULL REFERENCES inventory_locations(id),
    status                  VARCHAR(30) NOT NULL DEFAULT 'REQUESTED',
    requested_by            UUID        NOT NULL,
    approved_by             UUID,
    completed_at            TIMESTAMPTZ,
    is_active               BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_inv_transfer_status CHECK (status IN ('REQUESTED','APPROVED','IN_TRANSIT','RECEIVED','CANCELLED')),
    CONSTRAINT chk_inv_transfer_diff_locations CHECK (source_location_id <> destination_location_id)
);

CREATE INDEX IF NOT EXISTS idx_inv_transfers_source_location      ON inventory_transfers(source_location_id);
CREATE INDEX IF NOT EXISTS idx_inv_transfers_destination_location ON inventory_transfers(destination_location_id);
CREATE INDEX IF NOT EXISTS idx_inv_transfers_status               ON inventory_transfers(status);
CREATE INDEX IF NOT EXISTS idx_inv_transfers_requested_by         ON inventory_transfers(requested_by);

--changeset fynza:025-06 labels:inventory dbms:postgresql
CREATE TABLE IF NOT EXISTS inventory_transfer_items (
    id                BIGSERIAL   PRIMARY KEY,
    transfer_id       BIGINT      NOT NULL REFERENCES inventory_transfers(id) ON DELETE CASCADE,
    inventory_id      BIGINT      NOT NULL REFERENCES inventory(id),
    quantity          INT         NOT NULL,
    received_quantity INT         NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT chk_inv_transfer_item_quantity_pos   CHECK (quantity > 0),
    CONSTRAINT chk_inv_transfer_item_received_valid CHECK (received_quantity >= 0 AND received_quantity <= quantity)
);

CREATE INDEX IF NOT EXISTS idx_inv_transfer_items_transfer_id  ON inventory_transfer_items(transfer_id);
CREATE INDEX IF NOT EXISTS idx_inv_transfer_items_inventory_id ON inventory_transfer_items(inventory_id);
