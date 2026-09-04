package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.EmployeeTodayStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TodayStatusResponse {

    private AttendanceStatus status;
    private LocalDateTime firstBadgeIn;
    private LocalDateTime lastBadgeOut;
    private Integer totalDurationMinutes;

    @JsonProperty("isLate")
    private Boolean isLate;

    private Integer minutesLate;

    @JsonProperty("hoursReached")
    private Boolean hoursReached;

    public static TodayStatusResponse from(EmployeeTodayStatus row) {
        return TodayStatusResponse.builder()
                .status(parseStatus(row.getStatus()))
                .firstBadgeIn(row.getFirstBadgeIn())
                .lastBadgeOut(row.getLastBadgeOut())
                .totalDurationMinutes(row.getTotalDurationMinutes())
                .isLate(row.getIsLate())
                .minutesLate(row.getMinutesLate())
                .hoursReached(row.getHoursReached())
                .build();
    }

    private static AttendanceStatus parseStatus(String status) {
        if (status == null) return null;
        try {
            return AttendanceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AttendanceStatus.ABSENT;
        }
    }
}
