package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "employee_attendance_self_view")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeAttendanceSelfView {

    @Id
    @Column(name = "record_id", updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "building_id")
    private UUID buildingId;

    @Column(name = "office_id")
    private UUID officeId;

    @Column(name = "record_date")
    private LocalDate recordDate;

    @Column(name = "status")
    private String status;

    @Column(name = "work_session_id")
    private UUID workSessionId;

    @Column(name = "remote_request_id")
    private UUID remoteRequestId;

    @Column(name = "leave_request_id")
    private UUID leaveRequestId;

    @Column(name = "is_overridden")
    private Boolean isOverridden;

    @Column(name = "original_status")
    private String originalStatus;

    @Column(name = "override_by")
    private UUID overrideBy;

    @Column(name = "override_reason")
    private String overrideReason;

    @Column(name = "overridden_at")
    private LocalDateTime overriddenAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Joined from work_session
    @Column(name = "first_badge_in")
    private LocalDateTime firstBadgeIn;

    @Column(name = "last_badge_out")
    private LocalDateTime lastBadgeOut;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(name = "session_split_count")
    private Integer sessionSplitCount;

    @Column(name = "crosses_midnight")
    private Boolean crossesMidnight;

    @Column(name = "is_late")
    private Boolean isLate;

    @Column(name = "hours_reached")
    private Boolean hoursReached;
}
