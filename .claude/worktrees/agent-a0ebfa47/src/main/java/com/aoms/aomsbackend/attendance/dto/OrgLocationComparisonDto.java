package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrgLocationComparisonDto {
    UUID organisationId;
    UUID buildingId;
    String buildingName;
    String cityName;
    LocalDate recordDate;
    int year;
    int month;
    long totalEmployeesWithRecord;
    long inOfficeCount;
    long remoteCount;
    long onLeaveCount;
    long absentCount;
    BigDecimal attendanceRatePct;
}
