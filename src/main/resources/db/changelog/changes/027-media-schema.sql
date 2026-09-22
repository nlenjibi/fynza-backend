--liquibase formatted sql

-- =============================================================================
-- 027 — Media & Image Storage Management Schema
-- Module: MEDIA
-- Milestone: M1 (Foundation) + M2 (Upload Sessions) + M3 (Variants) + M4 (Quotas)
-- =============================================================================

--changeset fynza:027-01 labels:media dbms:postgresql
CREATE TABLE IF NOT EXISTS media_assets (
    id                BIGSERIAL    PRIMARY KEY,
    public_id         UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    owner_id          UUID         NOT NULL,
    owner_type        VARCHAR(50)  NOT NULL,
    provider          VARCHAR(50)  NOT NULL,
    bucket            VARCHAR(255),
    object_key        TEXT         NOT NULL,
    original_filename VARCHAR(500),
    stored_filename   VARCHAR(500),
    mime_type         VARCHAR(100) NOT NULL,
    media_type        VARCHAR(50)  NOT NULL,
    file_size         BIGINT       NOT NULL,
    width             INTEGER,
    height            INTEGER,
    checksum          VARCHAR(255),
    etag              VARCHAR(255),
    provider_asset_id VARCHAR(500),
    visibility        VARCHAR(30)  NOT NULL DEFAULT 'PUBLIC',
    status            VARCHAR(30)  NOT NULL DEFAULT 'PENDING',
    upload_method     VARCHAR(30),
    cdn_url           TEXT,
    uploaded_by       UUID         NOT NULL,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted_at        TIMESTAMPTZ,

    CONSTRAINT chk_media_asset_visibility CHECK (visibility IN ('PUBLIC','PRIVATE','AUTHENTICATED','OWNER_ONLY','SELLER_ONLY','ADMIN_ONLY')),
    CONSTRAINT chk_media_asset_status     CHECK (status IN ('PENDING','UPLOADING','UPLOADED','PROCESSING','READY','FAILED','EXPIRED','DELETING','DELETED','QUARANTINED')),
    CONSTRAINT chk_media_asset_owner_type CHECK (owner_type IN ('PRODUCT','VARIANT','SELLER','USER','REVIEW','CATEGORY','STORE','BANNER','DOCUMENT')),
    CONSTRAINT chk_media_asset_file_size  CHECK (file_size > 0)
);

CREATE INDEX IF NOT EXISTS idx_media_assets_owner         ON media_assets(owner_id, owner_type);
CREATE INDEX IF NOT EXISTS idx_media_assets_status        ON media_assets(status);
CREATE INDEX IF NOT EXISTS idx_media_assets_media_type    ON media_assets(media_type);
CREATE INDEX IF NOT EXISTS idx_media_assets_uploaded_by   ON media_assets(uploaded_by);
CREATE INDEX IF NOT EXISTS idx_media_assets_is_active     ON media_assets(is_active);
CREATE INDEX IF NOT EXISTS idx_media_assets_deleted_at    ON media_assets(deleted_at);

--changeset fynza:027-02 labels:media dbms:postgresql
CREATE TABLE IF NOT EXISTS media_upload_sessions (
    id              BIGSERIAL    PRIMARY KEY,
    public_id       UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    media_asset_id  BIGINT       REFERENCES media_assets(id),
    provider        VARCHAR(50)  NOT NULL,
    object_key      TEXT         NOT NULL,
    filename        VARCHAR(500) NOT NULL,
    mime_type       VARCHAR(100) NOT NULL,
    expected_size   BIGINT       NOT NULL,
    actual_size     BIGINT,
    checksum        VARCHAR(255),
    status          VARCHAR(30)  NOT NULL DEFAULT 'CREATED',
    upload_url      TEXT,
    expires_at      TIMESTAMPTZ  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    completed_at    TIMESTAMPTZ,

    CONSTRAINT chk_upload_session_status CHECK (status IN ('CREATED','AUTHORIZED','UPLOADING','COMPLETED','VERIFIED','MEDIA_CREATED','EXPIRED','FAILED','CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_upload_sessions_user_id    ON media_upload_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_status     ON media_upload_sessions(status);
CREATE INDEX IF NOT EXISTS idx_upload_sessions_expires_at ON media_upload_sessions(expires_at);

--changeset fynza:027-03 labels:media dbms:postgresql
-- Variants: thumbnail, small, medium, large generated from the original asset.
CREATE TABLE IF NOT EXISTS media_variants (
    id             BIGSERIAL    PRIMARY KEY,
    media_asset_id BIGINT       NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    variant_name   VARCHAR(50)  NOT NULL,
    width          INTEGER,
    height         INTEGER,
    format         VARCHAR(20),
    provider       VARCHAR(50)  NOT NULL,
    object_key     TEXT         NOT NULL,
    file_size      BIGINT,
    url            TEXT,
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT chk_media_variant_name   CHECK (variant_name IN ('ORIGINAL','THUMBNAIL','SMALL','MEDIUM','LARGE')),
    CONSTRAINT chk_media_variant_status CHECK (status IN ('PENDING','PROCESSING','READY','FAILED')),
    CONSTRAINT uq_media_variant         UNIQUE (media_asset_id, variant_name)
);

CREATE INDEX IF NOT EXISTS idx_media_variants_asset_id ON media_variants(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_media_variants_status   ON media_variants(status);

--changeset fynza:027-04 labels:media dbms:postgresql
-- Replace the placeholder product_media table (019, used media_id VARCHAR) with
-- the FK-linked version that references the new media_assets table.
DROP TABLE IF EXISTS product_media CASCADE;
CREATE TABLE IF NOT EXISTS product_media (
    id             BIGSERIAL   PRIMARY KEY,
    product_id     UUID        NOT NULL,
    media_asset_id BIGINT      NOT NULL REFERENCES media_assets(id),
    sort_order     INT         NOT NULL DEFAULT 0,
    is_primary     BOOLEAN     NOT NULL DEFAULT FALSE,
    alt_text       VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_product_media UNIQUE (product_id, media_asset_id)
);

CREATE INDEX IF NOT EXISTS idx_product_media_product_id     ON product_media(product_id);
CREATE INDEX IF NOT EXISTS idx_product_media_media_asset_id ON product_media(media_asset_id);
CREATE INDEX IF NOT EXISTS idx_product_media_is_primary     ON product_media(product_id, is_primary);

--changeset fynza:027-05 labels:media dbms:postgresql
CREATE TABLE IF NOT EXISTS storage_usage (
    id              BIGSERIAL   PRIMARY KEY,
    owner_id        UUID        NOT NULL,
    owner_type      VARCHAR(50) NOT NULL,
    storage_bytes   BIGINT      NOT NULL DEFAULT 0,
    object_count    BIGINT      NOT NULL DEFAULT 0,
    bandwidth_bytes BIGINT      NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT uq_storage_usage_owner UNIQUE (owner_id, owner_type),
    CONSTRAINT chk_storage_usage_bytes   CHECK (storage_bytes   >= 0),
    CONSTRAINT chk_storage_usage_count   CHECK (object_count    >= 0),
    CONSTRAINT chk_storage_usage_bw      CHECK (bandwidth_bytes >= 0)
);

CREATE INDEX IF NOT EXISTS idx_storage_usage_owner ON storage_usage(owner_id, owner_type);
