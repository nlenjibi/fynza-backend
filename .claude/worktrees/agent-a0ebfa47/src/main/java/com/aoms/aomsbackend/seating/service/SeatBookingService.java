package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.attendance.dto.CreateSeatBookingRequest;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.seating.dto.BookingResponse;
import com.aoms.aomsbackend.seating.dto.CreateBookingRequest;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public interface SeatBookingService {

    BookingResponse createBooking(HttpServletRequest request, CreateBookingRequest req);

    List<BookingResponse> getMyBookings(HttpServletRequest request);

    BookingResponse getBookingById(HttpServletRequest request, UUID bookingId);

    SeatBookingResponse cancelBooking(UUID buildingId, UUID bookingId,
                                      UUID actingUserId, UserRoleType actingRole,
                                      String cancellationReason);

    SeatBookingResponse getBooking(UUID buildingId, UUID bookingId,
                                   UUID actingUserId, UserRoleType actingRole);
    /**
     * Books a seat for the given employee on the requested date.
     *
     * <p>If an existing CONFIRMED placeholder booking (from a block reservation) exists for
     * the seat and date, the placeholder's {@code userId} is updated to the employee's ID
     * in-place, preserving the {@code blockReservationId}. Otherwise a new booking is created,
     * provided no CONFIRMED booking already exists for that seat and date.
     *
     * @param employeeId the UUID of the authenticated employee
     * @param request    the seat booking request
     * @return the resulting seat booking
     * @throws com.aoms.aomsbackend.common.exception.BadRequestException if the seat is already booked
     */
    SeatBookingResponse createSeatBooking(UUID employeeId, CreateSeatBookingRequest request);
}
