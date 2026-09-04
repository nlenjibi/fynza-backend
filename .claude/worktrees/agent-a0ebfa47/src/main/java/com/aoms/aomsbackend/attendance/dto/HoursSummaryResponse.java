package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.EmployeeHoursSummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HoursSummaryResponse {

    private UUID userId;
    private Integer year;
    private Integer month;
    private Integer inOfficeDays;
    private Double totalHoursWorked;
    private Double avgDailyHours;
    private Integer hoursReachedDays;
    private Integer hoursMissedDays;
    private Integer longestSessionMinutes;
    private Integer shortestSessionMinutes;
    private Integer minPresenceMinutes;

    public static HoursSummaryResponse from(EmployeeHoursSummary row) {
        return HoursSummaryResponse.builder()
                .userId(row.getUserId())
                .year(row.getYear())
                .month(row.getMonth())
                .inOfficeDays(row.getInOfficeDays())
                .totalHoursWorked(row.getTotalHoursWorked())
                .avgDailyHours(row.getAvgDailyHours())
                .hoursReachedDays(row.getHoursReachedDays())
                .hoursMissedDays(row.getHoursMissedDays())
                .longestSessionMinutes(row.getLongestSessionMinutes())
                .shortestSessionMinutes(row.getShortestSessionMinutes())
                .minPresenceMinutes(row.getMinPresenceMinutes())
                .build();
    }
}
