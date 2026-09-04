package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrgDailySummaryDto {
    UUID organisationId;
    String countryName;
    UUID buildingId;
    String buildingName;
    UUID officeId;
    String cityName;
    LocalDate recordDate;
    long totalEmployeesWithRecord;
    long inOfficeCount;
    long presentCount;
    long lateCount;
    long insufficientHoursCount;
    long remoteCount;
    long onLeaveCount;
    long absentCount;
    BigDecimal attendanceRatePct;
}
