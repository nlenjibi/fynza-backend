package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.attendance.dto.CancelBookingRequest;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.auth.service.UserRoleAccessService;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.config.util.SessionUtils;
import com.aoms.aomsbackend.seating.service.SeatBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for hot-desk seat booking operations.
 *
 * <p>All endpoints are scoped to a specific building via the {@code {buildingId}} path variable,
 * which is validated by the {@link com.aoms.aomsbackend.config.interceptor.LocationRoleInterceptor}
 * using {@code @RequiresRole}.
 */
@RestController
@RequestMapping("/api/v1/locations/{buildingId}/bookings")
@RequiredArgsConstructor
@Tag(name = "Seat Bookings", description = "Hot-desk seat booking management")
public class HotDeskBookingController {

    private final SeatBookingService seatBookingService;
    private final UserRoleAccessService userRoleAccessService;

    @GetMapping("/{bookingId}")
    @RequiresRole(UserRoleType.EMPLOYEE)
    @Operation(
            summary = "Get booking detail",
            description = "Returns booking detail. Employees see only their own booking; "
                    + "Facilities Admin and above see any booking at their location.",
        responses = {
                @ApiResponse(responseCode = "200", description = "Booking returned successfully"),
                @ApiResponse(responseCode = "403", description = "Not your booking and insufficient admin role"),
                @ApiResponse(responseCode = "404", description = "Booking not found at this location")
            }
    )
    public ResponseEntity<ResponseWrapper<SeatBookingResponse>> getBooking(
            @PathVariable UUID buildingId,
            @PathVariable UUID bookingId
    ) {
        UUID actingUserId = SessionUtils.extractUserId();
        UserRoleType actingRole = resolveActingRole(actingUserId, buildingId);
        return ResponseEntity.ok(ResponseWrapper.success(
                seatBookingService.getBooking(buildingId, bookingId, actingUserId, actingRole)));
    }

    @DeleteMapping("/{bookingId}")
    @RequiresRole(UserRoleType.EMPLOYEE)
    @Operation(
            summary = "Cancel a seat booking",
            description = "Cancels a CONFIRMED hot-desk booking. "
                    + "Employees may only cancel their own booking before the cutoff window. "
                    + "Facilities Admin and Super Admin may cancel any booking without the cutoff restriction.",
    responses = {
            @ApiResponse(responseCode = "200", description = "Booking cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Cancellation cutoff has passed (CANCELLATION_CUTOFF_PASSED)"),
            @ApiResponse(responseCode = "403", description = "Cannot cancel another user's booking"),
            @ApiResponse(responseCode = "404", description = "Booking not found at this location"),
            @ApiResponse(responseCode = "409", description = "Booking is already cancelled or auto-released (BOOKING_ALREADY_INACTIVE)")
    })
    public ResponseEntity<ResponseWrapper<SeatBookingResponse>> cancelBooking(
            @PathVariable UUID buildingId,
            @PathVariable UUID bookingId,
            @Valid @RequestBody(required = false) CancelBookingRequest request
    ) {
        UUID actingUserId = SessionUtils.extractUserId();
        UserRoleType actingRole = resolveActingRole(actingUserId, buildingId);
        String reason = (request != null) ? request.getCancellationReason() : null;
        return ResponseEntity.ok(ResponseWrapper.success(
                seatBookingService.cancelBooking(buildingId, bookingId, actingUserId, actingRole, reason)));
    }

    /**
     * Determines the highest role of the acting user at the given building by walking
     * the role rank ladder from SUPER_ADMIN down to EMPLOYEE.
     *
     * <p>FACILITIES_ADMIN and MANAGER are checked before HR because the cancellation
     * service grants admin-override privileges starting at FACILITIES_ADMIN — resolving
     * an HR user as HR (rather than FACILITIES_ADMIN) ensures they do not inadvertently
     * receive that bypass.
     */
    private UserRoleType resolveActingRole(UUID userId, UUID buildingId) {
        if (userRoleAccessService.hasAccess(userId, buildingId, UserRoleType.SUPER_ADMIN)) {
            return UserRoleType.SUPER_ADMIN;
        }
        if (userRoleAccessService.hasAccess(userId, buildingId, UserRoleType.FACILITIES_ADMIN)) {
            return UserRoleType.FACILITIES_ADMIN;
        }
        if (userRoleAccessService.hasAccess(userId, buildingId, UserRoleType.MANAGER)) {
            return UserRoleType.MANAGER;
        }
        if (userRoleAccessService.hasAccess(userId, buildingId, UserRoleType.HR)) {
            return UserRoleType.HR;
        }
        return UserRoleType.EMPLOYEE;
    }
}
