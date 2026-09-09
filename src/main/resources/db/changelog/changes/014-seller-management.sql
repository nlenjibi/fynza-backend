--liquibase formatted sql
--changeset fynza:014-seller-management dbms:postgresql

CREATE TABLE IF NOT EXISTS sellers (
    id              BIGSERIAL       PRIMARY KEY,
    public_id       UUID            NOT NULL UNIQUE,
    seller_number   VARCHAR(20)     NOT NULL UNIQUE,
    owner_user_id   UUID            NOT NULL REFERENCES users(id),
    seller_type     VARCHAR(30)     NOT NULL DEFAULT 'INDIVIDUAL',
    status          VARCHAR(30)     NOT NULL DEFAULT 'DRAFT',
    display_name    VARCHAR(255),
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP       NOT NULL,
    updated_at      TIMESTAMP       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_sellers_public_id   ON sellers(public_id);
CREATE INDEX IF NOT EXISTS idx_sellers_number      ON sellers(seller_number);
CREATE INDEX IF NOT EXISTS idx_sellers_owner       ON sellers(owner_user_id);
CREATE INDEX IF NOT EXISTS idx_sellers_status      ON sellers(status);
CREATE INDEX IF NOT EXISTS idx_sellers_created_at  ON sellers(created_at);

CREATE TABLE IF NOT EXISTS seller_businesses (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL UNIQUE,
    seller_id           BIGINT          NOT NULL REFERENCES sellers(id),
    legal_name          VARCHAR(255),
    business_name       VARCHAR(255),
    business_type       VARCHAR(50),
    registration_number VARCHAR(100),
    tax_identifier      VARCHAR(100),
    description         TEXT,
    website             VARCHAR(500),
    email               VARCHAR(255),
    phone               VARCHAR(50),
    country             VARCHAR(100),
    region              VARCHAR(100),
    city                VARCHAR(100),
    address             VARCHAR(500),
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_seller_business_seller ON seller_businesses(seller_id);

CREATE TABLE IF NOT EXISTS seller_status_history (
    id              BIGSERIAL       PRIMARY KEY,
    seller_id       BIGINT          NOT NULL REFERENCES sellers(id),
    previous_status VARCHAR(30),
    new_status      VARCHAR(30)     NOT NULL,
    reason          TEXT,
    changed_by      UUID,
    expires_at      TIMESTAMP,
    created_at      TIMESTAMP       NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_seller_status_hist_seller ON seller_status_history(seller_id);

CREATE TABLE IF NOT EXISTS seller_verifications (
    id                  BIGSERIAL       PRIMARY KEY,
    public_id           UUID            NOT NULL UNIQUE,
    seller_id           BIGINT          NOT NULL REFERENCES sellers(id),
    verification_type   VARCHAR(50)     NOT NULL,
    status              VARCHAR(30)     NOT NULL DEFAULT 'NOT_STARTED',
    submitted_at        TIMESTAMP,
    reviewed_at         TIMESTAMP,
    reviewed_by         UUID,
    rejection_reason    TEXT,
    expires_at          TIMESTAMP,
    is_active           BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL,
    updated_at          TIMESTAMP       NOT NULL,
    CONSTRAINT uq_seller_verification_type UNIQUE (seller_id, verification_type)
);

CREATE INDEX IF NOT EXISTS idx_seller_verif_seller ON seller_verifications(seller_id);
CREATE INDEX IF NOT EXISTS idx_seller_verif_type   ON seller_verifications(verification_type);
CREATE INDEX IF NOT EXISTS idx_seller_verif_status ON seller_verifications(status);

--rollback DROP TABLE IF EXISTS seller_verifications CASCADE;
--rollback DROP TABLE IF EXISTS seller_status_history CASCADE;
--rollback DROP TABLE IF EXISTS seller_businesses CASCADE;
--rollback DROP TABLE IF EXISTS sellers CASCADE;
