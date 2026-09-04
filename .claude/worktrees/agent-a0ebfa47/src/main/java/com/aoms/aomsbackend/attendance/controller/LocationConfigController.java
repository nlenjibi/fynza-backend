package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.LocationConfigResponse;
import com.aoms.aomsbackend.attendance.dto.LocationConfigUpdateRequest;
import com.aoms.aomsbackend.attendance.service.LocationConfigService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.exception.BadRequestException;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.config.util.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * The type Location config controller.
 */
@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location Config", description = "Manage location presence thresholds")
public class LocationConfigController {

    private final LocationConfigService service;

    /**
     * Gets config.
     *
     * @param buildingId the building id
     * @return the config
     */
    @GetMapping("/{buildingId}/config")
    @RequiresRole(UserRoleType.EMPLOYEE)
    @Operation(
            summary = "Get location config",
            description = "Returns the full configuration for the given building. Requires EMPLOYEE role or above for that building.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Config returned successfully"),
                    @ApiResponse(responseCode = "403", description = "Not authenticated or no role for this building"),
                    @ApiResponse(responseCode = "404", description = "No config found for this building")
            }
    )
    public ResponseEntity<ResponseWrapper<LocationConfigResponse>> getConfig(@PathVariable UUID buildingId) {
        return ResponseEntity.ok(ResponseWrapper.success(service.getByBuildingId(buildingId)));
    }

    /**
     * Update config response entity.
     *
     * @param buildingId the building id
     * @param request    the request
     * @return the response entity
     */
    @PatchMapping("/{buildingId}/config")
    @RequiresRole(UserRoleType.HR)
    @Operation(
            summary = "Update location config",
            description = "Updates minPresenceDurationMinutes for the given building. Restricted to HR users only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Config updated successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failed — minPresenceDurationMinutes must be a positive integer"),
                    @ApiResponse(responseCode = "403", description = "Not an HR user for this building"),
                    @ApiResponse(responseCode = "404", description = "No config found for this building")
            }
    )
    public ResponseEntity<ResponseWrapper<LocationConfigResponse>> updateConfig(
            @PathVariable UUID buildingId,
            @RequestBody @Valid LocationConfigUpdateRequest request) {
        if (request.isEmpty()) {
            throw new BadRequestException("At least one field must be provided for update");
        }
        return ResponseEntity.ok(ResponseWrapper.success(service.updateByBuildingId(buildingId, request)));
    }

    @PatchMapping("/{locationId}/config/seat-visibility")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    @Operation(
            summary = "Update seat visibility mode",
            description = "Sets how occupancy information is shown on the floor plan. " +
                    "Accessible to Facilities Admin (own location only) and Super Admin (any location).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Seat visibility mode updated"),
                    @ApiResponse(responseCode = "400", description = "Invalid visibility mode value"),
                    @ApiResponse(responseCode = "403", description = "Not a Facilities Admin or Super Admin for this location"),
                    @ApiResponse(responseCode = "404", description = "No config found for this location")
            }
    )
    public ResponseEntity<ResponseWrapper<LocationConfigResponse>> updateSeatVisibility(
            @PathVariable UUID locationId,
            @RequestBody @Valid UpdateSeatVisibilityRequest request) {
        UUID actorId = SessionUtils.extractUserId();
        return ResponseEntity.ok(ResponseWrapper.success(
                "Seat visibility mode updated",
                service.updateSeatVisibility(locationId, request, actorId)));
    }
}
