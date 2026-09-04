package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.EmployeePunctualitySummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PunctualitySummaryResponse {

    private UUID userId;
    private Integer year;
    private Integer month;
    private Integer inOfficeDays;
    private Integer onTimeDays;
    private Integer lateDays;
    private Double onTimeRatePct;
    private Double avgMinutesLate;
    private Integer maxMinutesLate;
    private LocalDateTime earliestArrival;
    private LocalDateTime latestArrival;

    public static PunctualitySummaryResponse from(EmployeePunctualitySummary row) {
        return PunctualitySummaryResponse.builder()
                .userId(row.getUserId())
                .year(row.getYear())
                .month(row.getMonth())
                .inOfficeDays(row.getInOfficeDays())
                .onTimeDays(row.getOnTimeDays())
                .lateDays(row.getLateDays())
                .onTimeRatePct(row.getOnTimeRatePct())
                .avgMinutesLate(row.getAvgMinutesLate())
                .maxMinutesLate(row.getMaxMinutesLate())
                .earliestArrival(row.getEarliestArrival())
                .latestArrival(row.getLatestArrival())
                .build();
    }
}
