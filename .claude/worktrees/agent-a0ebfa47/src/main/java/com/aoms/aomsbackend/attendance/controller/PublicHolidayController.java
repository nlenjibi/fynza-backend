package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.DeleteHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayCreateRequest;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayResponse;
import com.aoms.aomsbackend.attendance.dto.PublicHolidayUpdateRequest;
import com.aoms.aomsbackend.attendance.service.PublicHolidayService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.config.util.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing public holidays per office location.
 * All endpoints are scoped to a specific location via the {@code {locationId}} path variable,
 * which is enforced by the {@code LocationRoleInterceptor} using {@code @RequiresRole}.
 */
@RestController
@RequestMapping("/api/v1/locations/{locationId}/public-holidays")
@RequiredArgsConstructor
@Tag(name = "Public Holidays", description = "Manage public holidays per office location")
public class PublicHolidayController {

    private final PublicHolidayService publicHolidayService;
    private final UserRoleAccessService userRoleAccessService;

    /**
     * Creates a new public holiday for the given location.
     * HR users cannot create holidays in the past; SUPER_ADMIN may.
     *
     * @param locationId UUID of the target location (from path)
     * @param request    holiday creation payload (date + name)
     * @return 201 Created with the new holiday
     */
    @PostMapping
    @RequiresRole(UserRoleType.HR)
    @Operation(
            summary = "Create public holiday",
            description = "Creates a public holiday for the location. HR users cannot create past holidays; SUPER_ADMIN may.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Holiday created successfully"),
                    @ApiResponse(responseCode = "400", description = "Holiday date is in the past (HR only) or validation failed"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role for this location"),
                    @ApiResponse(responseCode = "409", description = "Holiday already exists on this date")
            }
    )
    public ResponseEntity<ResponseWrapper<PublicHolidayResponse>> create(
            @PathVariable UUID locationId,
            @RequestBody @Valid PublicHolidayCreateRequest request) {
        UUID actingUserId = SessionUtils.extractUserId();
        UserRoleType actingRole = resolveActingRole(actingUserId, locationId);
        PublicHolidayResponse response = publicHolidayService.create(locationId, request, actingUserId, actingRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success("Holiday created", response));
    }

    /**
     * Returns all public holidays for the given location, sorted by date ascending.
     * Optionally filtered to a single calendar year via the {@code year} query parameter.
     *
     * @param locationId UUID of the target location (from path)
     * @param year       optional calendar year filter (e.g. {@code ?year=2026})
     * @return 200 OK with list of holidays
     */
    @GetMapping
    @RequiresRole(UserRoleType.EMPLOYEE)
    @Operation(
            summary = "List public holidays",
            description = "Returns all holidays for the location sorted by date. Filter by year using ?year=YYYY.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Holidays returned successfully"),
                    @ApiResponse(responseCode = "403", description = "Not authenticated or insufficient role")
            }
    )
    public ResponseEntity<ResponseWrapper<List<PublicHolidayResponse>>> list(
            @PathVariable UUID locationId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(ResponseWrapper.success(publicHolidayService.list(locationId, year)));
    }

    /**
     * Updates the name and/or date of an existing future public holiday.
     * Past holidays cannot be edited.
     *
     * @param locationId UUID of the target location (from path)
     * @param holidayId  UUID of the holiday to update (from path)
     * @param request    update payload (name and/or date — both optional)
     * @return 200 OK with the updated holiday
     */
    @PutMapping("/{holidayId}")
    @RequiresRole(UserRoleType.HR)
    @Operation(
            summary = "Update public holiday",
            description = "Updates the name and/or date of a future holiday. Past holidays cannot be edited.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Holiday updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Holiday is in the past (PAST_HOLIDAY_IMMUTABLE) or validation failed"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role for this location"),
                    @ApiResponse(responseCode = "404", description = "Holiday not found"),
                    @ApiResponse(responseCode = "409", description = "Another holiday already exists on the requested date")
            }
    )
    public ResponseEntity<ResponseWrapper<PublicHolidayResponse>> update(
            @PathVariable UUID locationId,
            @PathVariable UUID holidayId,
            @RequestBody @Valid PublicHolidayUpdateRequest request) {
        return ResponseEntity.ok(ResponseWrapper.success(
                publicHolidayService.update(locationId, holidayId, request)));
    }

    /**
     * Deletes a public holiday.
     * Future holidays are deleted immediately ({@code restampQueued: false}).
     * Past holidays may only be deleted by SUPER_ADMIN, which also queues a Pass 2 restamp
     * for the affected date ({@code restampQueued: true}). HR attempting to delete a past
     * holiday receives 403.
     *
     * @param locationId   UUID of the target location (from path)
     * @param holidayId    UUID of the holiday to delete (from path)
     * @return 200 OK with {@link DeleteHolidayResponse} indicating whether a restamp was queued
     */
    @DeleteMapping("/{holidayId}")
    @RequiresRole(UserRoleType.HR)
    @Operation(
            summary = "Delete public holiday",
            description = "Deletes a holiday. Past holidays require SUPER_ADMIN role and trigger a restamp.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Holiday deleted; body indicates whether restamp was queued"),
                    @ApiResponse(responseCode = "403", description = "HR attempting to delete a past holiday, or insufficient role"),
                    @ApiResponse(responseCode = "404", description = "Holiday not found")
            }
    )
    public ResponseEntity<ResponseWrapper<DeleteHolidayResponse>> delete(
            @PathVariable UUID locationId,
            @PathVariable UUID holidayId
    ) {
        UUID actingUserId = SessionUtils.extractUserId();
        UserRoleType actingRole = resolveActingRole(actingUserId, locationId);
        DeleteHolidayResponse response = publicHolidayService.delete(locationId, holidayId, actingRole);
        return ResponseEntity.ok(ResponseWrapper.success(response));
    }

    /**
     * Determines the highest role of the acting user at the given location.
     *
     * @param userId     UUID of the authenticated user
     * @param locationId UUID of the location being acted upon
     * @return SUPER_ADMIN if the user holds that role at this location, otherwise HR
     */
    private UserRoleType resolveActingRole(UUID userId, UUID locationId) {
        boolean isSuperAdmin = userRoleAccessService.hasAccess(userId, locationId, UserRoleType.SUPER_ADMIN);
        return isSuperAdmin ? UserRoleType.SUPER_ADMIN : UserRoleType.HR;
    }
}
