package com.aoms.aomsbackend.seating.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
public class ZoneResponse {

    UUID id;
    UUID floorId;
    UUID buildingId;
    String name;
    @JsonProperty("isActive")
    boolean active;
    Instant createdAt;
    Instant updatedAt;
}
