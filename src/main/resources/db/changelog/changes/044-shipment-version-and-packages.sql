--liquibase formatted sql

--changeset fynza:044-shipment-version
ALTER TABLE shipments ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE shipments DROP COLUMN IF EXISTS version;

--changeset fynza:044-packages-table splitStatements:false
CREATE TABLE IF NOT EXISTS packages (
    id              BIGSERIAL PRIMARY KEY,
    public_id       UUID          NOT NULL UNIQUE,
    shipment_id     UUID          NOT NULL REFERENCES shipments(public_id),
    package_number  VARCHAR(30)   NOT NULL,
    weight_kg       NUMERIC(10,3),
    length_cm       NUMERIC(10,2),
    width_cm        NUMERIC(10,2),
    height_cm       NUMERIC(10,2),
    package_type    VARCHAR(20)   NOT NULL DEFAULT 'BOX',
    label_reference VARCHAR(255),
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL,
    updated_at      TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_packages_shipment_id ON packages(shipment_id);
--rollback DROP TABLE IF EXISTS packages CASCADE;
