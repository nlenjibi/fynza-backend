package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response representation of a public holiday.
 * Exposes {@code locationId} (mapped from the entity's internal {@code buildingId} field)
 * to align with the API path convention {@code /api/v1/locations/{locationId}/public-holidays}.
 */
@Value
@Builder
public class PublicHolidayResponse {

    UUID id;

    /** The location this holiday belongs to (maps to entity.buildingId). */
    UUID locationId;

    LocalDate holidayDate;

    String name;

    UUID createdBy;

    LocalDateTime createdAt;
}
