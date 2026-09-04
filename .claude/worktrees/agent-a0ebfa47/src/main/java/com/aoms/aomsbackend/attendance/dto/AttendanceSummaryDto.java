package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class AttendanceSummaryDto {
    LocalDate date;
    long totalPresent;
    long totalLate;
    long totalAbsent;
    long totalRemote;
    long totalOnLeave;
    long totalPublicHoliday;
    long totalInsufficientHours;
}
