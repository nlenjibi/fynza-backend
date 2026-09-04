package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.seating.dto.request.CreateZoneRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateZoneRequest;
import com.aoms.aomsbackend.seating.dto.response.ZoneResponse;
import com.aoms.aomsbackend.seating.service.ZoneService;
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
@RequestMapping("/api/v1/buildings/{buildingId}/floors/{floorId}/zones")
@RequiredArgsConstructor
@Tag(name = "Zones", description = "Zone management within a floor")
public class ZoneController {

    private final ZoneService zoneService;

    @Operation(summary = "List all active zones for a floor")
    @GetMapping
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<List<ZoneResponse>>> listZones(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId) {
        return ResponseEntity.ok(ResponseWrapper.success(zoneService.listZones(buildingId, floorId)));
    }

    @Operation(summary = "Get a zone by ID")
    @GetMapping("/{zoneId}")
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<ZoneResponse>> getZone(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId) {
        return ResponseEntity.ok(ResponseWrapper.success(zoneService.getZone(buildingId, floorId, zoneId)));
    }

    @Operation(summary = "Create a new zone")
    @PostMapping
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<ZoneResponse>> createZone(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @Valid @RequestBody CreateZoneRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success("Zone created successfully", zoneService.createZone(buildingId, floorId, request)));
    }

    @Operation(summary = "Update a zone")
    @PutMapping("/{zoneId}")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<ZoneResponse>> updateZone(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId,
            @Valid @RequestBody UpdateZoneRequest request) {
        return ResponseEntity.ok(ResponseWrapper.success("Zone updated successfully", zoneService.updateZone(buildingId, floorId, zoneId, request)));
    }

    @Operation(summary = "Deactivate a zone and all its seats")
    @DeleteMapping("/{zoneId}")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<Void> deactivateZone(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId) {
        zoneService.deactivateZone(buildingId, floorId, zoneId);
        return ResponseEntity.noContent().build();
    }
}
