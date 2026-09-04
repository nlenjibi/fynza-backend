package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class LocationLatenessSummaryDto {
    UUID buildingId;
    UUID employeeId;
    String employeeName;
    String employeeCode;
    String department;
    UUID managerId;
    int year;
    int month;
    long lateDays;
    BigDecimal avgMinutesLate;
    Integer maxMinutesLate;
    long totalDaysWithRecord;
    BigDecimal lateRatePct;
}
