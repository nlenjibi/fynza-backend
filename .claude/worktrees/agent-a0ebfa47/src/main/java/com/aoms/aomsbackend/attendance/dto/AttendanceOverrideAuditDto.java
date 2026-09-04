package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AttendanceOverrideAuditDto {
    UUID organisationId;
    UUID buildingId;
    String buildingName;
    UUID recordId;
    LocalDate recordDate;
    int year;
    int month;
    UUID employeeId;
    String employeeName;
    String employeeCode;
    String department;
    String originalStatus;
    String currentStatus;
    String overrideReason;
    OffsetDateTime overriddenAt;
    UUID overrideBy;
    String overriddenByName;
    String overriderJobTitle;
}
