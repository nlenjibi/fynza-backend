package com.aoms.aomsbackend.seating.controller;

import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.seating.dto.AssignPermanentSeatRequest;
import com.aoms.aomsbackend.seating.dto.SeatAssignmentResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeResponse;
import com.aoms.aomsbackend.seating.dto.SeatTypeUpdateRequest;
import com.aoms.aomsbackend.seating.service.SeatManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
@RequiresRole(UserRoleType.FACILITIES_ADMIN)
@Tag(name = "Seat Management", description = "Facilities admin seat assignment and type conversion endpoints")
public class SeatManagementController {

    private final SeatManagementService seatManagementService;

    @Operation(
            summary = "Assign a permanent seat to an employee",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Assignment created"),
                    @ApiResponse(responseCode = "400", description = "Seat is not PERMANENT type or employee not valid"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role"),
                    @ApiResponse(responseCode = "404", description = "Seat not found"),
                    @ApiResponse(responseCode = "409", description = "Seat already assigned")
            }
    )
    @PostMapping("/{seatId}/permanent-assignment")
    public ResponseEntity<ResponseWrapper<SeatAssignmentResponse>> assign(
            @Parameter(description = "UUID of the seat to assign.")
            @PathVariable UUID seatId,
            @Valid @RequestBody AssignPermanentSeatRequest req,
            HttpServletRequest request) {

        SeatAssignmentResponse result = seatManagementService.assign(seatId, req, request);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Unassign a permanent seat",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Assignment removed"),
                    @ApiResponse(responseCode = "400", description = "Seat has no current assignment"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role"),
                    @ApiResponse(responseCode = "404", description = "Seat not found")
            }
    )
    @DeleteMapping("/{seatId}/permanent-assignment")
    public ResponseEntity<ResponseWrapper<SeatAssignmentResponse>> unassign(
            @Parameter(description = "UUID of the seat to unassign.")
            @PathVariable UUID seatId,
            HttpServletRequest request) {

        SeatAssignmentResponse result = seatManagementService.unassign(seatId, request);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Convert seat type",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Type updated"),
                    @ApiResponse(responseCode = "400", description = "Conversion blocked — seat still assigned or has future bookings"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role"),
                    @ApiResponse(responseCode = "404", description = "Seat not found")
            }
    )
    @PatchMapping("/{seatId}/type")
    public ResponseEntity<ResponseWrapper<SeatTypeResponse>> convertType(
            @Parameter(description = "UUID of the seat to convert.")
            @PathVariable UUID seatId,
            @Valid @RequestBody SeatTypeUpdateRequest req,
            HttpServletRequest request) {

        SeatTypeResponse result = seatManagementService.convertType(seatId, req, request);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }
}
