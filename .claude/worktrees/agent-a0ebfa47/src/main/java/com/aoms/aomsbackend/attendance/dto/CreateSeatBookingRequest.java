package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateSeatBookingRequest {

    @NotNull(message = "seatId is required")
    private UUID seatId;

    @NotNull(message = "bookingDate is required")
    @Future(message = "bookingDate must be a future date")
    private LocalDate bookingDate;

    @NotNull(message = "buildingId is required")
    private UUID buildingId;
}
