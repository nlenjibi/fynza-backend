--liquibase formatted sql

--changeset aoms:007-create-seats
CREATE TABLE IF NOT EXISTS seats (
    id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    organisation_id  UUID         NOT NULL,
    zone_id          UUID,
    seat_number      VARCHAR(20),
    seat_label       VARCHAR(50),
    seat_type        VARCHAR(20)  NOT NULL,
    status           VARCHAR(20)  NOT NULL DEFAULT 'AVAILABLE',
    permanent_user_id UUID,
    deleted_at       TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_seats_organisation ON seats (organisation_id);
CREATE INDEX IF NOT EXISTS idx_seats_zone         ON seats (zone_id);
CREATE INDEX IF NOT EXISTS idx_seats_type_status  ON seats (seat_type, status);
