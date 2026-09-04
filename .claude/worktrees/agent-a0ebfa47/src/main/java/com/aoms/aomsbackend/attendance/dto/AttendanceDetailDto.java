package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AttendanceDetailDto {
    UUID employeeId;
    String employeeFullName;
    String employeeCode;
    String department;
    String jobTitle;
    String team;
    UUID buildingId;
    LocalDate recordDate;
    String status;
    OffsetDateTime firstBadgeIn;
    OffsetDateTime lastBadgeOut;
    Integer totalDurationMinutes;
    Boolean isLate;
    Integer minutesLate;
    Boolean isOverridden;
    String overrideReason;
}
