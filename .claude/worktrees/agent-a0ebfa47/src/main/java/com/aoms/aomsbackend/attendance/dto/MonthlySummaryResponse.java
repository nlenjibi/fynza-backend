package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceMonthlySummary;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryResponse {

    private UUID userId;
    private Integer year;
    private Integer month;
    private Integer daysWithRecord;
    private Integer inOfficeDays;
    private Integer presentDays;
    private Integer lateDays;
    private Integer insufficientHoursDays;
    private Integer remoteDays;
    private Integer absentDays;
    private Integer onLeaveDays;
    private Integer publicHolidayDays;
    private Double totalHoursWorked;
    private Double avgDailyHours;
    private Integer hoursReachedDays;
    private Double attendanceRatePct;

    public static MonthlySummaryResponse from(EmployeeAttendanceMonthlySummary row) {
        return MonthlySummaryResponse.builder()
                .userId(row.getUserId())
                .year(row.getYear())
                .month(row.getMonth())
                .daysWithRecord(row.getDaysWithRecord())
                .inOfficeDays(row.getInOfficeDays())
                .presentDays(row.getPresentDays())
                .lateDays(row.getLateDays())
                .insufficientHoursDays(row.getInsufficientHoursDays())
                .remoteDays(row.getRemoteDays())
                .absentDays(row.getAbsentDays())
                .onLeaveDays(row.getOnLeaveDays())
                .publicHolidayDays(row.getPublicHolidayDays())
                .totalHoursWorked(row.getTotalHoursWorked())
                .avgDailyHours(row.getAvgDailyHours())
                .hoursReachedDays(row.getHoursReachedDays())
                .attendanceRatePct(row.getAttendanceRatePct())
                .build();
    }
}
