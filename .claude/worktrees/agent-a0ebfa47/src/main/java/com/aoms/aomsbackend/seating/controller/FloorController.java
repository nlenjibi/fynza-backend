package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.seating.dto.request.CreateFloorRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateFloorRequest;
import com.aoms.aomsbackend.seating.dto.response.FloorResponse;
import com.aoms.aomsbackend.seating.service.FloorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/floors")
@RequiredArgsConstructor
@Tag(name = "Floors", description = "Floor management within a building")
public class FloorController {

    private final FloorService floorService;

    @Operation(summary = "List all active floors for a building")
    @GetMapping
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<List<FloorResponse>>> listFloors(
            @PathVariable UUID buildingId) {
        return ResponseEntity.ok(ResponseWrapper.success(floorService.listFloors(buildingId)));
    }

    @Operation(summary = "Get a floor by ID")
    @GetMapping("/{floorId}")
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<FloorResponse>> getFloor(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId) {
        return ResponseEntity.ok(ResponseWrapper.success(floorService.getFloor(buildingId, floorId)));
    }

    @Operation(summary = "Create a new floor")
    @PostMapping
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<FloorResponse>> createFloor(
            @PathVariable UUID buildingId,
            @Valid @RequestBody CreateFloorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success("Floor created successfully", floorService.createFloor(buildingId, request)));
    }

    @Operation(summary = "Update a floor")
    @PutMapping("/{floorId}")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<FloorResponse>> updateFloor(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @Valid @RequestBody UpdateFloorRequest request) {
        return ResponseEntity.ok(ResponseWrapper.success("Floor updated successfully", floorService.updateFloor(buildingId, floorId, request)));
    }

    @Operation(summary = "Deactivate a floor and all its zones and seats")
    @DeleteMapping("/{floorId}")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<Void> deactivateFloor(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId) {
        floorService.deactivateFloor(buildingId, floorId);
        return ResponseEntity.noContent().build();
    }
}
