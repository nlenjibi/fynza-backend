package com.aoms.aomsbackend.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyRemoteUsageResponse {

    private Integer remoteDaysUsed;
    private Integer weeklyLimit;
    private Integer daysRemaining;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private double fillPct;

    public static WeeklyRemoteUsageResponse from(EmployeeWeeklyRemoteUsage row) {
        double fillPct = Math.min((double) row.getRemoteDaysUsed() / row.getWeeklyLimit(), 1.0) * 100;
        return WeeklyRemoteUsageResponse.builder()
                .remoteDaysUsed(row.getRemoteDaysUsed())
                .weeklyLimit(row.getWeeklyLimit())
                .daysRemaining(row.getDaysRemaining())
                .weekStartDate(row.getWeekStartDate())
                .weekEndDate(row.getWeekEndDate())
                .fillPct(fillPct)
                .build();
    }
}
