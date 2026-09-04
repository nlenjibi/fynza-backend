--liquibase formatted sql
--changeset aoms:007-seat-visibility

UPDATE location_config
SET seat_visibility_mode = 'FULL'
WHERE seat_visibility_mode = 'ALL' OR seat_visibility_mode IS NULL;

CREATE TABLE location_config_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    building_id UUID NOT NULL REFERENCES office_building(id),
    previous_mode VARCHAR(20),
    new_mode    VARCHAR(20) NOT NULL,
    changed_by  UUID NOT NULL REFERENCES users(id),
    changed_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_location_config_history_building_id ON location_config_history(building_id);
CREATE INDEX idx_location_config_history_changed_at  ON location_config_history(changed_at);
