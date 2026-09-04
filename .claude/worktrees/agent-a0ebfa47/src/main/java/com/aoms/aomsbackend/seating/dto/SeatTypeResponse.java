package com.aoms.aomsbackend.seating.dto;

import com.aoms.aomsbackend.seating.entity.SeatType;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class SeatTypeResponse {

    private UUID seatId;
    private SeatType seatType;
}
