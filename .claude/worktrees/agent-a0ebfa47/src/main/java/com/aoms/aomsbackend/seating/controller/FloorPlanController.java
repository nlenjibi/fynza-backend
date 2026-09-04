package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.config.util.SessionUtils;
import com.aoms.aomsbackend.seating.dto.FloorPlanResponse;
import com.aoms.aomsbackend.seating.service.FloorPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Floor Plan", description = "Floor plan occupancy view with visibility-mode filtering")
public class FloorPlanController {

    private final FloorPlanService floorPlanService;

    @GetMapping("/{locationId}/floor-plan")
    @Operation(
            summary = "Get floor plan",
            description = "Returns all floors and seats for a building. Occupant information is " +
                    "filtered according to the location's current seatVisibilityMode: " +
                    "FULL shows all occupants, TEAM_ONLY shows only same-department occupants, " +
                    "AVAILABILITY_ONLY shows seats as occupied/available with no names.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Floor plan returned"),
                    @ApiResponse(responseCode = "401", description = "Unauthenticated"),
                    @ApiResponse(responseCode = "404", description = "No config found for this location")
            }
    )
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<FloorPlanResponse>> getFloorPlan(
            @PathVariable UUID locationId) {
        UUID requestingUserId = SessionUtils.extractUserId();
        return ResponseEntity.ok(ResponseWrapper.success(
                floorPlanService.getFloorPlan(locationId, requestingUserId)));
    }
}
