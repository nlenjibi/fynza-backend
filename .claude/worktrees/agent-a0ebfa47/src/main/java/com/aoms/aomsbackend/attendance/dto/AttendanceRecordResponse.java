package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceRecordResponse {

    private UUID id;
    private LocalDate recordDate;
    private AttendanceStatus status;
    private LocalDateTime firstBadgeIn;
    private LocalDateTime lastBadgeOut;
    private Integer totalDurationMinutes;

    @JsonProperty("isLate")
    private Boolean isLate;

    private Integer minutesLate;

    @JsonProperty("isOverridden")
    private Boolean isOverridden;

    @JsonProperty("hoursReached")
    private Boolean hoursReached;
}
