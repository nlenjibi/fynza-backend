package com.aoms.aomsbackend.seating.dto.request;

import com.aoms.aomsbackend.seating.entity.SeatType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSeatRequest {

    @NotBlank
    private String seatNumber;

    @NotNull
    private SeatType seatType;

    private Float xPosition;

    private Float yPosition;
}
