package com.aoms.aomsbackend.attendance.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttendanceRecordRevertRequest {

    @NotBlank(message = "revertReason is required")
    private String revertReason;
}
