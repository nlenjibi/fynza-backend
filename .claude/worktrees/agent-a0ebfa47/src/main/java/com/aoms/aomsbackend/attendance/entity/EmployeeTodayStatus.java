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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@Table(name = "employee_today_status")
public class EmployeeTodayStatus {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "status")
    private String status;

    @Column(name = "first_badge_in")
    private LocalDateTime firstBadgeIn;

    @Column(name = "last_badge_out")
    private LocalDateTime lastBadgeOut;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "is_late")
    private Boolean isLate;

    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(name = "hours_reached")
    private Boolean hoursReached;
}
