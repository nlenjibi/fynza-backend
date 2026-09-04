package com.aoms.aomsbackend.seating.dto.response;

import com.aoms.aomsbackend.seating.entity.SeatStatus;
import com.aoms.aomsbackend.seating.entity.SeatType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class SeatResponse {

    UUID id;
    UUID zoneId;
    UUID floorId;
    UUID buildingId;
    String seatNumber;
    SeatType seatType;
    SeatStatus status;
    UUID assignedEmployeeId;
    Float xPosition;
    Float yPosition;
    @JsonProperty("isActive")
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}
