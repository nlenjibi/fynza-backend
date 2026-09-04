--liquibase formatted sql

--changeset aoms:004-add-department-to-users
ALTER TABLE users ADD COLUMN IF NOT EXISTS department VARCHAR(100);

--changeset aoms:004-create-no-show-record-read-model
CREATE TABLE IF NOT EXISTS no_show_record_read_model (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    no_show_record_id   UUID NOT NULL UNIQUE,
    user_id             UUID NOT NULL,
    organisation_id     UUID,
    booking_date        DATE NOT NULL,
    seat_reference      VARCHAR(255) NOT NULL,
    auto_released_at    TIMESTAMPTZ NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_no_show_user_id        ON no_show_record_read_model(user_id);
CREATE INDEX IF NOT EXISTS idx_no_show_organisation    ON no_show_record_read_model(organisation_id);
CREATE INDEX IF NOT EXISTS idx_no_show_booking_date    ON no_show_record_read_model(booking_date);

--changeset aoms:004-seed-no-show-records
INSERT INTO no_show_record_read_model
    (id, no_show_record_id, user_id, organisation_id, booking_date, seat_reference, auto_released_at)
VALUES
    ('aa0e8400-e29b-41d4-a716-446655440001',
     'bb0e8400-e29b-41d4-a716-446655440001',
     '550e8400-e29b-41d4-a716-446655440006',
     NULL,
     CURRENT_DATE - 10, 'Floor 2 / Zone A / Seat 14', now() - INTERVAL '10 days'),
    ('aa0e8400-e29b-41d4-a716-446655440002',
     'bb0e8400-e29b-41d4-a716-446655440002',
     '550e8400-e29b-41d4-a716-446655440006',
     NULL,
     CURRENT_DATE - 7,  'Floor 2 / Zone A / Seat 14', now() - INTERVAL '7 days'),
    ('aa0e8400-e29b-41d4-a716-446655440003',
     'bb0e8400-e29b-41d4-a716-446655440003',
     '550e8400-e29b-41d4-a716-446655440007',
     NULL,
     CURRENT_DATE - 8,  'Floor 1 / Zone B / Seat 5',  now() - INTERVAL '8 days'),
    ('aa0e8400-e29b-41d4-a716-446655440004',
     'bb0e8400-e29b-41d4-a716-446655440004',
     '550e8400-e29b-41d4-a716-446655440008',
     NULL,
     CURRENT_DATE - 5,  'Floor 3 / Zone C / Seat 9',  now() - INTERVAL '5 days'),
    ('aa0e8400-e29b-41d4-a716-446655440005',
     'bb0e8400-e29b-41d4-a716-446655440005',
     '550e8400-e29b-41d4-a716-446655440009',
     NULL,
     CURRENT_DATE - 3,  'Floor 1 / Zone A / Seat 2',  now() - INTERVAL '3 days')
ON CONFLICT (id) DO NOTHING;
