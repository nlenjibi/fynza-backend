--liquibase formatted sql

--changeset aoms:005-create-audit-log
CREATE TABLE IF NOT EXISTS audit_log (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    action       VARCHAR(100) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    entity_id    UUID,
    performed_by UUID,
    details      TEXT,
    performed_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_entity       ON audit_log (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_performed_by ON audit_log (performed_by);

--changeset aoms:005-create-seat-booking
CREATE TABLE IF NOT EXISTS seat_booking (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    seat_id         UUID        NOT NULL,
    booking_date    DATE        NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED',
    organisation_id UUID,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_seat_booking_user_id  ON seat_booking (user_id);
CREATE INDEX IF NOT EXISTS idx_seat_booking_seat_id  ON seat_booking (seat_id);
CREATE INDEX IF NOT EXISTS idx_seat_booking_date     ON seat_booking (booking_date);
CREATE INDEX IF NOT EXISTS idx_seat_booking_org_date ON seat_booking (organisation_id, booking_date);

CREATE UNIQUE INDEX IF NOT EXISTS uq_seat_booking_confirmed
    ON seat_booking (seat_id, booking_date) WHERE (status = 'CONFIRMED');
