--liquibase formatted sql
--changeset aoms:006-seating-hierarchy

ALTER TABLE floor ADD COLUMN deleted_at TIMESTAMPTZ;

CREATE TABLE zone (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    floor_id    UUID NOT NULL REFERENCES floor(id),
    building_id UUID NOT NULL REFERENCES office_building(id),
    name        VARCHAR(100) NOT NULL,
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted_at  TIMESTAMPTZ
);

CREATE INDEX idx_zone_floor_id    ON zone(floor_id);
CREATE INDEX idx_zone_building_id ON zone(building_id);
CREATE INDEX idx_zone_is_active   ON zone(is_active);

ALTER TABLE seat ADD COLUMN zone_id   UUID REFERENCES zone(id);
ALTER TABLE seat ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE seat ALTER COLUMN room_id DROP NOT NULL;

INSERT INTO zone (id, floor_id, building_id, name, is_active, created_at, updated_at)
SELECT gen_random_uuid(), f.id, f.building_id, 'Default Zone', TRUE, NOW(), NOW()
FROM floor f
WHERE EXISTS (
    SELECT 1
    FROM seat s
    WHERE s.floor_id = f.id AND s.zone_id IS NULL
);

UPDATE seat s
SET zone_id = z.id
FROM zone z
WHERE s.zone_id IS NULL
  AND z.floor_id = s.floor_id
  AND z.name = 'Default Zone';

CREATE INDEX idx_seat_zone_id   ON seat(zone_id);
CREATE INDEX idx_seat_is_active ON seat(is_active);

CREATE UNIQUE INDEX idx_unique_active_seat_per_zone
ON seat(zone_id, seat_number)
WHERE is_active = true;
