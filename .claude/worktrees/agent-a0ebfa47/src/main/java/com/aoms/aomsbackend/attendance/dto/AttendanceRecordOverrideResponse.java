package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Builder
public class AttendanceRecordOverrideResponse {

    private UUID id;
    private UUID userId;
    private UUID buildingId;
    private LocalDate recordDate;
    private AttendanceStatus status;
    private UUID workSessionId;
    private Boolean isOverridden;
    private String overrideReason;
    private AttendanceStatus originalStatus;
    private String revertReasons;
    private UUID overriddenBy;
    private OffsetDateTime overriddenAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
