package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@AllArgsConstructor
public class ChronicAbsenteeismDto {
    UUID organisationId;
    UUID buildingId;
    UUID employeeId;
    String employeeName;
    String employeeCode;
    String department;
    UUID managerId;
    int year;
    int month;
    long absentDays;
    long inOfficeDays;
    long totalDaysWithRecord;
    BigDecimal absenceRatePct;
}
