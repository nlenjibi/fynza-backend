package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AttendanceRecordOverrideRequest {

    @NotNull(message = "status is required")
    private AttendanceStatus status;

    @NotBlank(message = "overrideReason is required")
    @Size(max = 500, message = "overrideReason must not exceed 500 characters")
    private String overrideReason;
}
