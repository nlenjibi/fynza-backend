package com.aoms.aomsbackend.seating.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CreateBookingRequest {

    @NotNull(message = "Seat ID is required.")
    private UUID seatId;

    @NotNull(message = "Booking date is required.")
    private LocalDate bookingDate;
}
