--liquibase formatted sql

--changeset fynza:054-return-shipping-columns
ALTER TABLE returns ADD COLUMN IF NOT EXISTS return_shipment_id       UUID;
ALTER TABLE returns ADD COLUMN IF NOT EXISTS return_label_reference   VARCHAR(500);

CREATE INDEX IF NOT EXISTS idx_returns_return_shipment_id ON returns (return_shipment_id);
