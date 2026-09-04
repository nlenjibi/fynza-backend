package com.aoms.aomsbackend.seating.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class FloorPlanSeatResponse {
    UUID seatId;
    String seatNumber;
    String seatType;
    String status;
    Float xPosition;
    Float yPosition;
    @JsonProperty("isOccupied")
    boolean occupied;
    OccupantInfo occupantInfo;
}
