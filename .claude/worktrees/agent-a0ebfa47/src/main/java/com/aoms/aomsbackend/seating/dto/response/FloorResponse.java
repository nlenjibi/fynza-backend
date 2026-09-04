package com.aoms.aomsbackend.seating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class FloorResponse {

    UUID id;
    UUID buildingId;
    String name;
    Integer floorNumber;
    @JsonProperty("isActive")
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}
