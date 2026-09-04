package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.DeleteHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayCreateRequest;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayUpdateRequest;
import com.aoms.aomsbackend.auth.entity.UserRoleType;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing public holidays per office location.
 * Handles CRUD operations and coordinates audit event publishing and
 * Pass 2 restamp notifications for past-holiday deletions.
 */
public interface PublicHolidayService {

    /**
     * Creates a new public holiday for the given location.
     * HR users may only create future holidays; SUPER_ADMIN may create past ones.
     * Duplicate dates for the same location result in a 409 conflict.
     *
     * @param locationId  UUID of the location (building)
     * @param request     create request containing date and name
     * @param createdBy   UUID of the authenticated user performing the action
     * @param actingRole  role of the authenticated user (determines past-date permission)
     * @return the created holiday as a response DTO
     */
    PublicHolidayResponse create(UUID locationId, PublicHolidayCreateRequest request,
                                 UUID createdBy, UserRoleType actingRole);

    /**
     * Returns all public holidays for the given location, sorted by date ascending.
     * Optionally filtered to a specific calendar year via the {@code year} parameter.
     *
     * @param locationId UUID of the location (building)
     * @param year       optional calendar year filter (e.g. 2026); null returns all
     * @return list of holidays ordered by {@code holidayDate} ASC
     */
    List<PublicHolidayResponse> list(UUID locationId, Integer year);

    /**
     * Updates the name and/or date of an existing future public holiday.
     * Past holidays are immutable and result in a 400 error.
     *
     * @param locationId UUID of the location (building)
     * @param holidayId  UUID of the holiday to update
     * @param request    update request with optional name and/or date fields
     * @return the updated holiday as a response DTO
     */
    PublicHolidayResponse update(UUID locationId, UUID holidayId, PublicHolidayUpdateRequest request);

    /**
     * Hard-deletes a public holiday.
     * For future holidays: deletes immediately, returns {@code restampQueued=false}.
     * For past holidays: only SUPER_ADMIN may proceed; deletes and publishes a restamp
     * message to SNS so Pass 2 is re-run for the affected date, returns {@code restampQueued=true}.
     * HR attempting to delete a past holiday receives a 403 Forbidden.
     *
     * @param locationId  UUID of the location (building)
     * @param holidayId   UUID of the holiday to delete
     * @param actingRole  role of the authenticated user (determines past-deletion permission)
     * @return delete result indicating whether a restamp was queued
     */
    DeleteHolidayResponse delete(UUID locationId, UUID holidayId, UserRoleType actingRole);
}
