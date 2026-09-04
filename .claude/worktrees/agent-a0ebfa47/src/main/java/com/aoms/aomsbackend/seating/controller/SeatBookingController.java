package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.attendance.dto.CreateSeatBookingRequest;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.seating.dto.BookingResponse;
import com.aoms.aomsbackend.seating.service.SeatBookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for individual seat bookings.
 *
 * <p>The POST endpoint is block-reservation-aware: when a seat belongs to a block
 * reservation, the booking service automatically replaces the manager's placeholder
 * booking with the employee's booking in-place, preventing double-booking constraint
 * violations. Hot-desk seats without a placeholder are booked directly.</p>
 */
@RestController
@RequestMapping("/api/v1/seat-bookings")
@RequiredArgsConstructor
@Tag(name = "Seat Bookings", description = "Individual seat bookings including block-reservation seat claims")
public class SeatBookingController {

    private final SeatBookingService seatBookingService;

    /**
     * Books a seat for the authenticated employee.
     *
     * @param request     the seat booking details
     * @param httpRequest the HTTP request (used to read the authenticated employee's session)
     * @return 201 Created with the resulting seat booking
     */
    @PostMapping
    @RequiresRole(UserRoleType.EMPLOYEE)
    @Operation(
            summary = "Book a seat",
            description = "Books a seat for the authenticated employee on a given date. "
                    + "If the seat is part of a block reservation, the placeholder booking is claimed in-place. "
                    + "Requires EMPLOYEE role or above.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Seat booked successfully"),
                    @ApiResponse(responseCode = "400", description = "Seat is already booked for that date or validation failed"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "403", description = "Not authenticated or insufficient role")
            }
    )
    public ResponseEntity<ResponseWrapper<SeatBookingResponse>> createSeatBooking(
            @RequestBody @Valid CreateSeatBookingRequest request,
            HttpServletRequest httpRequest) {

        UUID employeeId = resolveUserId(httpRequest);
        SeatBookingResponse response = seatBookingService.createSeatBooking(employeeId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.success(response));
    }

    @Operation(
            summary = "List my seat bookings",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bookings returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my")
    public ResponseEntity<ResponseWrapper<List<BookingResponse>>> getMyBookings(
            HttpServletRequest request) {

        List<BookingResponse> result = seatBookingService.getMyBookings(request);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get a specific booking by ID",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Booking found"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "404", description = "Booking not found")
            }
    )
    @GetMapping("/my/{bookingId}")
    public ResponseEntity<ResponseWrapper<BookingResponse>> getBookingById(
            HttpServletRequest request,
            @PathVariable UUID bookingId) {

        BookingResponse result = seatBookingService.getBookingById(request, bookingId);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    private UUID resolveUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException();
        }
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            throw new SessionExpiredException();
        }
        return UUID.fromString(userId);
    }
}
