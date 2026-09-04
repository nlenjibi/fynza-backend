package com.aoms.aomsbackend.attendance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklyRemoteUsageHistoryResponse {

    private Integer weekNumber;
    private LocalDate weekStartDate;
    private LocalDate weekEndDate;
    private Integer remoteDaysUsed;
    private Integer weeklyLimit;

    @JsonProperty("overLimit")
    private Boolean overLimit;

    public static WeeklyRemoteUsageHistoryResponse from(EmployeeWeeklyRemoteUsageHistory row) {
        return WeeklyRemoteUsageHistoryResponse.builder()
                .weekNumber(row.getWeekNumber())
                .weekStartDate(row.getWeekStartDate())
                .weekEndDate(row.getWeekEndDate())
                .remoteDaysUsed(row.getRemoteDaysUsed())
                .weeklyLimit(row.getWeeklyLimit())
                .overLimit(row.getOverLimit())
                .build();
    }
}
