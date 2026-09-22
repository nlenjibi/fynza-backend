--liquibase formatted sql

--changeset fynza:043-drop-delivery-module-tables
--comment: Remove legacy delivery module tables superseded by shipping module
DROP TABLE IF EXISTS delivery_fees CASCADE;
DROP TABLE IF EXISTS delivery_regions CASCADE;
--rollback CREATE TABLE delivery_regions (id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL UNIQUE, name VARCHAR(100) NOT NULL, code VARCHAR(10) NOT NULL UNIQUE, country VARCHAR(100) NOT NULL DEFAULT 'Ghana', is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP);
--rollback CREATE TABLE delivery_fees (id BIGSERIAL PRIMARY KEY, public_id UUID NOT NULL UNIQUE, town_name VARCHAR(100) NOT NULL, delivery_method VARCHAR(30) NOT NULL DEFAULT 'DIRECT_ADDRESS', base_fee NUMERIC(10,2) NOT NULL DEFAULT 0, per_km_fee NUMERIC(10,2) NOT NULL DEFAULT 0, estimated_days INT NOT NULL DEFAULT 1, region_id UUID REFERENCES delivery_regions(id), is_active BOOLEAN NOT NULL DEFAULT TRUE, created_at TIMESTAMP NOT NULL, updated_at TIMESTAMP);
