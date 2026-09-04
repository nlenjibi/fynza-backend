package com.aoms.aomsbackend.seating.dto;

import com.aoms.aomsbackend.seating.entity.SeatType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SeatTypeUpdateRequest {

    @NotNull(message = "Seat type is required.")
    private SeatType seatType;
}
