package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
public class CreateBlockReservationRequest {

    @NotNull(message = "roomId is required")
    private UUID roomId;

    @NotNull(message = "reservationDate is required")
    @Future(message = "reservationDate must be a future date")
    private LocalDate reservationDate;

    @Min(value = 1, message = "seatCount must be at least 1")
    private int seatCount;

    private String notes;
}
