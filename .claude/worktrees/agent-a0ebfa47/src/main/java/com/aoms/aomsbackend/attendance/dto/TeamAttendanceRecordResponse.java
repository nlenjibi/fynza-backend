package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing a single attendance record for the team list endpoint.
 * Combines fields from {@code attendance_record} and the linked {@code work_session}.
 */
@Value
@Builder
public class TeamAttendanceRecordResponse {
    UUID employeeId;
    String employeeName;
    LocalDate recordDate;
    AttendanceStatus status;
    Instant firstBadgeIn;
    Instant lastBadgeOut;
    Integer totalDurationMinutes;
    Boolean isLate;
    Integer minutesLate;
    boolean isOverridden;
}
