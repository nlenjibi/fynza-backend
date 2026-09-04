package com.aoms.aomsbackend.seating.service;

import com.aoms.aomsbackend.seating.dto.FloorPlanResponse;

import java.util.UUID;

public interface FloorPlanService {

    FloorPlanResponse getFloorPlan(UUID buildingId, UUID requestingUserId);
}
