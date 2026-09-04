package com.aoms.aomsbackend.seating.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class FloorPlanFloorResponse {
    UUID floorId;
    String floorName;
    int floorNumber;
    List<FloorPlanSeatResponse> seats;
}
