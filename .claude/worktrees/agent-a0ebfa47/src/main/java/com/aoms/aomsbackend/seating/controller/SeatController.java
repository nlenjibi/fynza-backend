package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.seating.dto.request.CreateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatRequest;
import com.aoms.aomsbackend.seating.dto.request.UpdateSeatStatusRequest;
import com.aoms.aomsbackend.seating.dto.response.SeatResponse;
import com.aoms.aomsbackend.seating.service.SeatService;
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
@RequestMapping("/api/v1/buildings/{buildingId}/floors/{floorId}/zones/{zoneId}/seats")
@RequiredArgsConstructor
@Tag(name = "Seats", description = "Seat management within a zone")
public class SeatController {

    private final SeatService seatService;

    @Operation(summary = "List all active seats in a zone")
    @GetMapping
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<List<SeatResponse>>> listSeats(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId) {
        return ResponseEntity.ok(ResponseWrapper.success(seatService.listSeats(buildingId, floorId, zoneId)));
    }

    @Operation(summary = "Get a seat by ID")
    @GetMapping("/{seatId}")
    @RequiresRole(UserRoleType.EMPLOYEE)
    public ResponseEntity<ResponseWrapper<SeatResponse>> getSeat(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId,
            @PathVariable UUID seatId) {
        return ResponseEntity.ok(ResponseWrapper.success(seatService.getSeat(buildingId, floorId, zoneId, seatId)));
    }

    @Operation(summary = "Create a new seat")
    @PostMapping
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<SeatResponse>> createSeat(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId,
            @Valid @RequestBody CreateSeatRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.success("Seat created successfully", seatService.createSeat(buildingId, floorId, zoneId, request)));
    }

    @Operation(summary = "Update a seat")
    @PutMapping("/{seatId}")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<SeatResponse>> updateSeat(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatRequest request) {
        return ResponseEntity.ok(ResponseWrapper.success("Seat updated successfully", seatService.updateSeat(buildingId, floorId, zoneId, seatId, request)));
    }

    @Operation(summary = "Deactivate a seat")
    @DeleteMapping("/{seatId}")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<Void> deactivateSeat(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId,
            @PathVariable UUID seatId) {
        seatService.deactivateSeat(buildingId, floorId, zoneId, seatId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Update seat operational status")
    @PatchMapping("/{seatId}/status")
    @RequiresRole(UserRoleType.FACILITIES_ADMIN)
    public ResponseEntity<ResponseWrapper<SeatResponse>> updateSeatStatus(
            @PathVariable UUID buildingId,
            @PathVariable UUID floorId,
            @PathVariable UUID zoneId,
            @PathVariable UUID seatId,
            @Valid @RequestBody UpdateSeatStatusRequest request) {
        return ResponseEntity.ok(ResponseWrapper.success("Seat status updated", seatService.updateSeatStatus(buildingId, floorId, zoneId, seatId, request)));
    }
}
