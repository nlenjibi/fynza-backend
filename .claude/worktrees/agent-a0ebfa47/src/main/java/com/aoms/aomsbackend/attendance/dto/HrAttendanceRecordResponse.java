package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO representing a single attendance record for the HR location list endpoint.
 * Combines fields from {@code attendance_record}, the linked {@code work_session},
 * and the employee's name and department.
 */
@Value
@Builder
public class HrAttendanceRecordResponse {
    UUID employeeId;
    String employeeName;
    String department;
    LocalDate recordDate;
    AttendanceStatus status;
    Instant firstBadgeIn;
    Instant lastBadgeOut;
    Integer totalDurationMinutes;
    @JsonProperty("isLate") Boolean isLate;
    Integer minutesLate;
    @JsonProperty("isOverridden") boolean isOverridden;
}
