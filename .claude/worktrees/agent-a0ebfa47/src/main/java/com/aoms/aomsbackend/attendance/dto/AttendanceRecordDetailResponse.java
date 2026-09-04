package com.aoms.aomsbackend.attendance.dto;

import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceSelfView;
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
public class AttendanceRecordDetailResponse {

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

    private AttendanceStatus originalStatus;
    private LocalDateTime overriddenAt;
    private UUID overrideBy;
    private String overrideReason;

    private UUID workSessionId;
    private UUID leaveRequestId;
    private UUID remoteRequestId;

    private Integer sessionSplitCount;
    private Boolean crossesMidnight;

    public static AttendanceRecordDetailResponse from(EmployeeAttendanceSelfView row) {
        return builder()
                .id(row.getId())
                .recordDate(row.getRecordDate())
                .status(parseStatus(row.getStatus()))
                .firstBadgeIn(row.getFirstBadgeIn())
                .lastBadgeOut(row.getLastBadgeOut())
                .totalDurationMinutes(row.getTotalDurationMinutes())
                .isLate(row.getMinutesLate() != null && row.getMinutesLate() > 0)
                .minutesLate(row.getMinutesLate())
                .isOverridden(row.getIsOverridden())
                .originalStatus(parseStatus(row.getOriginalStatus()))
                .overriddenAt(row.getOverriddenAt())
                .overrideBy(row.getOverrideBy())
                .overrideReason(row.getOverrideReason())
                .workSessionId(row.getWorkSessionId())
                .leaveRequestId(row.getLeaveRequestId())
                .remoteRequestId(row.getRemoteRequestId())
                .sessionSplitCount(row.getSessionSplitCount())
                .crossesMidnight(row.getCrossesMidnight())
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
