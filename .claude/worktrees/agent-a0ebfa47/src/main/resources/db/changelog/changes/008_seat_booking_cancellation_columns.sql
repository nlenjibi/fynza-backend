--liquibase formatted sql

--changeset aoms:008-seat-booking-cancellation-columns
ALTER TABLE seat_booking
    ADD COLUMN IF NOT EXISTS building_id          UUID,
    ADD COLUMN IF NOT EXISTS block_reservation_id UUID,
    ADD COLUMN IF NOT EXISTS cancelled_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS cancellation_reason  TEXT,
    ADD COLUMN IF NOT EXISTS auto_released_at     TIMESTAMPTZ;

ALTER TABLE seat_booking
    ALTER COLUMN building_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_seat_booking_building_id ON seat_booking (building_id);
CREATE INDEX IF NOT EXISTS idx_seat_booking_status      ON seat_booking (status);
