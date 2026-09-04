package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrgEmployeeAttendanceDto {
    UUID organisationId;
    UUID buildingId;
    String buildingName;
    String cityName;
    UUID recordId;
    LocalDate recordDate;
    String status;
    Boolean isOverridden;
    String overrideReason;
    UUID employeeId;
    String employeeFullName;
    String employeeCode;
    String department;
    String jobTitle;
    String team;
    OffsetDateTime firstBadgeIn;
    OffsetDateTime lastBadgeOut;
    Integer totalDurationMinutes;
    Boolean isLate;
    Integer minutesLate;
}
