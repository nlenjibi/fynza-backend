package com.aoms.aomsbackend.seating.dto;

import com.aoms.aomsbackend.attendance.entity.SeatVisibilityMode;
import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.UUID;

@Value
@Builder
public class FloorPlanResponse {
    UUID buildingId;
    SeatVisibilityMode seatVisibilityMode;
    List<FloorPlanFloorResponse> floors;
}
