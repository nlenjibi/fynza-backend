--liquibase formatted sql
--changeset fynza:015-store-module dbms:postgresql

CREATE TABLE IF NOT EXISTS stores (
    id               BIGSERIAL    PRIMARY KEY,
    public_id        UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    seller_id        BIGINT       NOT NULL REFERENCES sellers(id),
    store_name       VARCHAR(255) NOT NULL,
    slug             VARCHAR(255) NOT NULL UNIQUE,
    description      TEXT,
    logo_media_id    VARCHAR(255),
    banner_media_id  VARCHAR(255),
    status           VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    visibility       VARCHAR(30)  NOT NULL DEFAULT 'PRIVATE',
    business_email   VARCHAR(255),
    business_phone   VARCHAR(50),
    website          VARCHAR(500),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_stores_public_id  ON stores(public_id);
CREATE INDEX IF NOT EXISTS idx_stores_seller_id  ON stores(seller_id);
CREATE INDEX IF NOT EXISTS idx_stores_slug       ON stores(slug);
CREATE INDEX IF NOT EXISTS idx_stores_status     ON stores(status);
CREATE INDEX IF NOT EXISTS idx_stores_visibility ON stores(visibility);
CREATE INDEX IF NOT EXISTS idx_stores_is_active  ON stores(is_active);

CREATE TABLE IF NOT EXISTS store_settings (
    id                     BIGSERIAL   PRIMARY KEY,
    public_id              UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    store_id               BIGINT      NOT NULL UNIQUE REFERENCES stores(id),
    currency               VARCHAR(3)  NOT NULL DEFAULT 'GHS',
    timezone               VARCHAR(50) NOT NULL DEFAULT 'Africa/Accra',
    language               VARCHAR(10) NOT NULL DEFAULT 'en',
    order_notifications    BOOLEAN     NOT NULL DEFAULT TRUE,
    customer_notifications BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at             TIMESTAMP   NOT NULL DEFAULT now(),
    updated_at             TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_store_settings_store_id ON store_settings(store_id);

CREATE TABLE IF NOT EXISTS store_policies (
    id             BIGSERIAL    PRIMARY KEY,
    public_id      UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    store_id       BIGINT       NOT NULL REFERENCES stores(id),
    type           VARCHAR(30)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    content        TEXT         NOT NULL,
    version        INTEGER      NOT NULL DEFAULT 1,
    status         VARCHAR(30)  NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMP,
    is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at     TIMESTAMP    NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_store_policies_store_id ON store_policies(store_id);
CREATE INDEX IF NOT EXISTS idx_store_policies_type     ON store_policies(store_id, type);
CREATE INDEX IF NOT EXISTS idx_store_policies_status   ON store_policies(status);

CREATE TABLE IF NOT EXISTS store_status_history (
    id              BIGSERIAL   PRIMARY KEY,
    public_id       UUID        NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    store_id        BIGINT      NOT NULL REFERENCES stores(id),
    previous_status VARCHAR(30),
    new_status      VARCHAR(30) NOT NULL,
    reason          TEXT,
    changed_by      UUID,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP   NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_store_status_hist_store   ON store_status_history(store_id);
CREATE INDEX IF NOT EXISTS idx_store_status_hist_created ON store_status_history(created_at);

CREATE TABLE IF NOT EXISTS store_slug_history (
    id         BIGSERIAL    PRIMARY KEY,
    store_id   BIGINT       NOT NULL REFERENCES stores(id),
    slug       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now(),
    expires_at TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_store_slug_hist_store ON store_slug_history(store_id);
CREATE INDEX IF NOT EXISTS idx_store_slug_hist_slug  ON store_slug_history(slug);

--rollback DROP TABLE IF EXISTS store_slug_history;
--rollback DROP TABLE IF EXISTS store_status_history;
--rollback DROP TABLE IF EXISTS store_policies;
--rollback DROP TABLE IF EXISTS store_settings;
--rollback DROP TABLE IF EXISTS stores;
