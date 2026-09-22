--liquibase formatted sql

--changeset fynza:041-drop-stale-shipping-if-wrong-schema
DO $$
BEGIN
    -- Drop all shipping tables if shipping_zones.id is UUID (stale schema from a prior
    -- iteration of this migration). Safe because: if id is BIGSERIAL the condition is
    -- false; if tables don't exist the condition is false.
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'shipping_zones'
          AND column_name = 'id'
          AND data_type = 'uuid'
    ) THEN
        DROP TABLE IF EXISTS shipping_webhook_events CASCADE;
        DROP TABLE IF EXISTS shipping_labels CASCADE;
        DROP TABLE IF EXISTS tracking_events CASCADE;
        DROP TABLE IF EXISTS shipment_items CASCADE;
        DROP TABLE IF EXISTS shipments CASCADE;
        DROP TABLE IF EXISTS fulfillments CASCADE;
        DROP TABLE IF EXISTS shipping_rates CASCADE;
        DROP TABLE IF EXISTS shipping_zones CASCADE;
        DROP TABLE IF EXISTS shipping_methods CASCADE;
        DROP TABLE IF EXISTS shipping_carriers CASCADE;
    END IF;
END $$;

--changeset fynza:041-shipping-carriers
CREATE TABLE IF NOT EXISTS shipping_carriers (
    id                      BIGSERIAL     PRIMARY KEY,
    public_id               UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name                    VARCHAR(100)  NOT NULL,
    code                    VARCHAR(20)   NOT NULL UNIQUE,
    logo_url                VARCHAR(500),
    tracking_url_template   VARCHAR(500),
    is_active               BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_shipping_carriers_code ON shipping_carriers(code);

--changeset fynza:041-shipping-methods
CREATE TABLE IF NOT EXISTS shipping_methods (
    id                  BIGSERIAL     PRIMARY KEY,
    public_id           UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    carrier_id          BIGINT        NOT NULL REFERENCES shipping_carriers(id) ON DELETE RESTRICT,
    name                VARCHAR(100)  NOT NULL,
    code                VARCHAR(50)   NOT NULL,
    description         TEXT,
    estimated_days_min  INTEGER       NOT NULL DEFAULT 1,
    estimated_days_max  INTEGER       NOT NULL DEFAULT 7,
    is_active           BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_shipping_method_carrier_code UNIQUE (carrier_id, code)
);

CREATE INDEX IF NOT EXISTS idx_shipping_methods_carrier_id ON shipping_methods(carrier_id);
CREATE INDEX IF NOT EXISTS idx_shipping_methods_code ON shipping_methods(code);

--changeset fynza:041-shipping-zones
CREATE TABLE IF NOT EXISTS shipping_zones (
    id          BIGSERIAL     PRIMARY KEY,
    public_id   UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    name        VARCHAR(100)  NOT NULL,
    description TEXT,
    regions     TEXT[]        NOT NULL DEFAULT '{}',
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

--changeset fynza:041-shipping-rates
CREATE TABLE IF NOT EXISTS shipping_rates (
    id                          BIGSERIAL     PRIMARY KEY,
    public_id                   UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    shipping_method_id          BIGINT        NOT NULL REFERENCES shipping_methods(id) ON DELETE RESTRICT,
    zone_id                     BIGINT        NOT NULL REFERENCES shipping_zones(id) ON DELETE RESTRICT,
    base_fee                    NUMERIC(19,4) NOT NULL DEFAULT 0,
    per_kg_fee                  NUMERIC(19,4) NOT NULL DEFAULT 0,
    free_shipping_threshold     NUMERIC(19,4),
    currency                    VARCHAR(3)    NOT NULL DEFAULT 'GHS',
    is_active                   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_shipping_rate_method_zone UNIQUE (shipping_method_id, zone_id)
);

CREATE INDEX IF NOT EXISTS idx_shipping_rates_method_id ON shipping_rates(shipping_method_id);
CREATE INDEX IF NOT EXISTS idx_shipping_rates_zone_id ON shipping_rates(zone_id);

--changeset fynza:041-fulfillments
CREATE TABLE IF NOT EXISTS fulfillments (
    id              BIGSERIAL     PRIMARY KEY,
    public_id       UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    order_id        UUID          NOT NULL,
    seller_order_id UUID          NOT NULL,
    seller_id       BIGINT        NOT NULL,
    status          VARCHAR(30)   NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING','PROCESSING','PACKED','READY_TO_SHIP','SHIPPED','COMPLETED','CANCELLED')),
    notes           TEXT,
    packed_at       TIMESTAMPTZ,
    shipped_at      TIMESTAMPTZ,
    completed_at    TIMESTAMPTZ,
    cancelled_at    TIMESTAMPTZ,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_fulfillments_order_id ON fulfillments(order_id);
CREATE INDEX IF NOT EXISTS idx_fulfillments_seller_order_id ON fulfillments(seller_order_id);
CREATE INDEX IF NOT EXISTS idx_fulfillments_seller_id ON fulfillments(seller_id);
CREATE INDEX IF NOT EXISTS idx_fulfillments_status ON fulfillments(status);

--changeset fynza:041-shipments
CREATE TABLE IF NOT EXISTS shipments (
    id                      BIGSERIAL     PRIMARY KEY,
    public_id               UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    shipment_number         VARCHAR(30)   NOT NULL UNIQUE,
    fulfillment_id          BIGINT        NOT NULL REFERENCES fulfillments(id) ON DELETE RESTRICT,
    carrier_id              BIGINT        REFERENCES shipping_carriers(id) ON DELETE SET NULL,
    shipping_method_id      BIGINT        REFERENCES shipping_methods(id) ON DELETE SET NULL,
    status                  VARCHAR(30)   NOT NULL DEFAULT 'DRAFT'
                                CHECK (status IN ('DRAFT','READY','LABEL_CREATED','PICKUP_SCHEDULED',
                                                  'PICKED_UP','IN_TRANSIT','OUT_FOR_DELIVERY','DELIVERED',
                                                  'DELIVERY_FAILED','RETURN_TO_SENDER','RETURNED',
                                                  'CANCELLED','LOST','DAMAGED')),
    tracking_number         VARCHAR(100),
    label_url               VARCHAR(500),
    estimated_delivery_date DATE,
    actual_delivery_date    DATE,
    -- recipient address snapshot
    recipient_name          VARCHAR(255),
    recipient_phone         VARCHAR(50),
    address_line1           VARCHAR(255),
    address_line2           VARCHAR(255),
    city                    VARCHAR(100),
    state                   VARCHAR(100),
    country                 VARCHAR(100)  DEFAULT 'Ghana',
    postal_code             VARCHAR(20),
    -- physical dimensions
    weight_kg               NUMERIC(8,3),
    length_cm               NUMERIC(8,2),
    width_cm                NUMERIC(8,2),
    height_cm               NUMERIC(8,2),
    -- cost
    shipping_cost           NUMERIC(19,4) NOT NULL DEFAULT 0,
    currency                VARCHAR(3)    NOT NULL DEFAULT 'GHS',
    -- lifecycle timestamps
    label_created_at        TIMESTAMPTZ,
    picked_up_at            TIMESTAMPTZ,
    delivered_at            TIMESTAMPTZ,
    cancelled_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_shipments_fulfillment_id ON shipments(fulfillment_id);
CREATE INDEX IF NOT EXISTS idx_shipments_status ON shipments(status);
CREATE INDEX IF NOT EXISTS idx_shipments_tracking_number ON shipments(tracking_number);
CREATE INDEX IF NOT EXISTS idx_shipments_carrier_id ON shipments(carrier_id);

--changeset fynza:041-shipment-items
CREATE TABLE IF NOT EXISTS shipment_items (
    id              BIGSERIAL     PRIMARY KEY,
    public_id       UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    shipment_id     BIGINT        NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    order_item_id   UUID          NOT NULL,
    product_id      UUID          NOT NULL,
    variant_id      UUID,
    product_name    VARCHAR(500)  NOT NULL,
    product_sku     VARCHAR(100),
    quantity        INTEGER       NOT NULL CHECK (quantity > 0),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_shipment_items_shipment_id ON shipment_items(shipment_id);

--changeset fynza:041-tracking-events
CREATE TABLE IF NOT EXISTS tracking_events (
    id          BIGSERIAL     PRIMARY KEY,
    public_id   UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    shipment_id BIGINT        NOT NULL REFERENCES shipments(id) ON DELETE CASCADE,
    status      VARCHAR(30)   NOT NULL,
    description TEXT,
    location    VARCHAR(255),
    occurred_at TIMESTAMPTZ   NOT NULL DEFAULT now(),
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_tracking_events_shipment_id ON tracking_events(shipment_id);
CREATE INDEX IF NOT EXISTS idx_tracking_events_occurred_at ON tracking_events(shipment_id, occurred_at DESC);

--changeset fynza:041-shipping-labels
CREATE TABLE IF NOT EXISTS shipping_labels (
    id                  BIGSERIAL     PRIMARY KEY,
    public_id           UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    shipment_id         BIGINT        NOT NULL UNIQUE REFERENCES shipments(id) ON DELETE CASCADE,
    label_url           VARCHAR(500)  NOT NULL,
    format              VARCHAR(10)   NOT NULL DEFAULT 'PDF',
    carrier_label_id    VARCHAR(200),
    expires_at          TIMESTAMPTZ,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now()
);

--changeset fynza:041-shipping-webhook-events
CREATE TABLE IF NOT EXISTS shipping_webhook_events (
    id                  BIGSERIAL     PRIMARY KEY,
    public_id           UUID          NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    provider            VARCHAR(50)   NOT NULL,
    provider_event_id   VARCHAR(200)  NOT NULL,
    event_type          VARCHAR(100)  NOT NULL,
    payload             JSONB         NOT NULL,
    processed           BOOLEAN       NOT NULL DEFAULT FALSE,
    processed_at        TIMESTAMPTZ,
    error_message       TEXT,
    created_at          TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT uq_webhook_event_provider_id UNIQUE (provider, provider_event_id)
);

CREATE INDEX IF NOT EXISTS idx_shipping_webhook_events_provider ON shipping_webhook_events(provider);
CREATE INDEX IF NOT EXISTS idx_shipping_webhook_events_processed ON shipping_webhook_events(processed) WHERE processed = FALSE;
