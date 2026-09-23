--liquibase formatted sql

--changeset fynza:055-return-items-inspection-columns
ALTER TABLE return_items ADD COLUMN IF NOT EXISTS approved_quantity INT;
ALTER TABLE return_items ADD COLUMN IF NOT EXISTS received_quantity INT;
ALTER TABLE return_items ADD COLUMN IF NOT EXISTS resolution       VARCHAR(20);

--changeset fynza:055-return-inspection-schema
CREATE TABLE IF NOT EXISTS return_inspections (
    id              BIGSERIAL       PRIMARY KEY,
    public_id       UUID            NOT NULL UNIQUE,
    return_id       UUID            NOT NULL,
    return_item_id  UUID,
    inspected_by    UUID,
    condition       VARCHAR(20)     NOT NULL,
    result          VARCHAR(20)     NOT NULL,
    notes           TEXT,
    inspected_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_inspections_return_id      ON return_inspections (return_id);
CREATE INDEX IF NOT EXISTS idx_return_inspections_return_item_id ON return_inspections (return_item_id);
CREATE INDEX IF NOT EXISTS idx_return_inspections_created_at     ON return_inspections (created_at);

--changeset fynza:055-return-disposition-schema
CREATE TABLE IF NOT EXISTS return_dispositions (
    id              BIGSERIAL       PRIMARY KEY,
    public_id       UUID            NOT NULL UNIQUE,
    return_id       UUID            NOT NULL,
    return_item_id  UUID            NOT NULL,
    type            VARCHAR(30)     NOT NULL,
    quantity        INT             NOT NULL DEFAULT 1,
    location_id     VARCHAR(100),
    reason          VARCHAR(500),
    processed_by    UUID,
    processed_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_return_dispositions_return_id      ON return_dispositions (return_id);
CREATE INDEX IF NOT EXISTS idx_return_dispositions_return_item_id ON return_dispositions (return_item_id);
