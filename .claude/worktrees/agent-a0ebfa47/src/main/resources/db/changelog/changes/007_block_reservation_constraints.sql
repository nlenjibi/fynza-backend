-- Add FK from seat_booking to block_reservation
ALTER TABLE seat_booking
    ADD CONSTRAINT fk_seat_booking_block_reservation
    FOREIGN KEY (block_reservation_id) REFERENCES block_reservation(id);

-- Enforce at most one CONFIRMED booking per seat per date.
-- CANCELLED and RELEASED bookings are exempt so historical records are preserved.
CREATE UNIQUE INDEX uq_seat_booking_confirmed
    ON seat_booking(seat_id, booking_date)
    WHERE status = 'CONFIRMED';
