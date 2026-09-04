package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class OrgDepartmentSummaryDto {
    UUID organisationId;
    UUID buildingId;
    String buildingName;
    LocalDate recordDate;
    String department;
    long totalEmployeesWithRecord;
    long inOfficeCount;
    long presentCount;
    long lateCount;
    long remoteCount;
    long onLeaveCount;
    long absentCount;
    BigDecimal attendanceRatePct;
}
