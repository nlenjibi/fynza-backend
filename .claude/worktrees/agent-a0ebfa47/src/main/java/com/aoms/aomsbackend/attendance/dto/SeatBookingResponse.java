package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.seating.entity.SeatBookingStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Immutable response DTO representing a hot-desk seat booking.
 */
@Value
@Builder
public class SeatBookingResponse {
    UUID id;
    UUID seatId;
    UUID userId;
    UUID buildingId;
    LocalDate bookingDate;
    SeatBookingStatus status;
    UUID blockReservationId;
    OffsetDateTime cancelledAt;
    String cancellationReason;
    OffsetDateTime autoReleasedAt;
    OffsetDateTime createdAt;
    OffsetDateTime updatedAt;
}
