package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideRequest;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordOverrideResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordRevertRequest;
import com.aoms.aomsbackend.attendance.service.AttendanceOverrideService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
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
 * The type Attendance override controller.
 */
@RestController
@RequestMapping("/api/v1/attendance-records")
@RequiredArgsConstructor
@RequiresRole(UserRoleType.SUPER_ADMIN)
@Tag(name = "Attendance Override", description = "SUPER_ADMIN override and revert of attendance records")
public class AttendanceOverrideController {

    private final AttendanceOverrideService service;

    @PatchMapping("/{id}/override")
    @Operation(
            summary = "Override an attendance record",
            description = "Replaces the pipeline-computed status with a manually supplied one. Only SUPER_ADMIN may call this endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record overridden successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failure — status or overrideReason missing/invalid"),
                    @ApiResponse(responseCode = "403", description = "Not a SUPER_ADMIN, or unauthenticated"),
                    @ApiResponse(responseCode = "404", description = "Record not found"),
                    @ApiResponse(responseCode = "409", description = "Record is already overridden")
            }
    )
    public ResponseEntity<ResponseWrapper<AttendanceRecordOverrideResponse>> override(
            @PathVariable UUID id,
            @RequestBody @Valid AttendanceRecordOverrideRequest request) {
        UUID actorId = SessionUtils.extractUserId();
        return ResponseEntity.ok(ResponseWrapper.success(service.override(id, request, actorId)));
    }

    @PatchMapping("/{id}/revert")
    @Operation(
            summary = "Revert an overridden attendance record",
            description = "Restores the original pipeline-computed status and clears all override fields. Only SUPER_ADMIN may call this endpoint.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record reverted successfully"),
                    @ApiResponse(responseCode = "400", description = "Validation failure — revertReason missing"),
                    @ApiResponse(responseCode = "403", description = "Not a SUPER_ADMIN, or unauthenticated"),
                    @ApiResponse(responseCode = "404", description = "Record not found"),
                    @ApiResponse(responseCode = "409", description = "Record is not overridden")
            }
    )
    public ResponseEntity<ResponseWrapper<AttendanceRecordOverrideResponse>> revert(
            @PathVariable UUID id,
            @RequestBody @Valid AttendanceRecordRevertRequest request) {
        UUID actorId = SessionUtils.extractUserId();
        return ResponseEntity.ok(ResponseWrapper.success(service.revert(id, request, actorId)));
    }
}
