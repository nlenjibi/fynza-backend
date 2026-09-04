package com.aoms.aomsbackend.attendance.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * DTO for the HR attendance daily summary endpoint.
 * All counts are derived from a single SQL aggregation — never computed in Java.
 */
@Value
@Builder
public class AttendanceSummaryResponse {
    LocalDate date;
    long totalPresent;
    long totalLate;
    long totalAbsent;
    long totalRemote;
    long totalOnLeave;
    long totalPublicHoliday;
    long totalInsufficientHours;
}
