package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class AttendanceExportDto {
    UUID employeeId;
    String employeeName;
    String employeeCode;
    String department;
    String team;
    String rank;
    UUID buildingId;
    LocalDate recordDate;
    String status;
    LocalDateTime firstBadgeInLocal;
    LocalDateTime lastBadgeOutLocal;
    Integer totalDurationMinutes;
    Integer minutesLate;
    Boolean isLate;
    Boolean isOverridden;
}
