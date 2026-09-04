--liquibase formatted sql

--changeset aoms:006-add-created-by-to-public-holiday
-- Adds the created_by column to track which user created each holiday.
-- Nullable because existing rows have no creator information.
ALTER TABLE public_holiday
    ADD COLUMN IF NOT EXISTS created_by UUID;

--rollback ALTER TABLE public_holiday DROP COLUMN IF EXISTS created_by;
