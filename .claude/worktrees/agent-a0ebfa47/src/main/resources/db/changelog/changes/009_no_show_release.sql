--liquibase formatted sql

--changeset aoms:009-no-show-release
ALTER TABLE no_show_record
    ADD CONSTRAINT uq_no_show_record_seat_booking_id UNIQUE (seat_booking_id);

ALTER TABLE job_execution_log
    ADD COLUMN records_released INTEGER;
