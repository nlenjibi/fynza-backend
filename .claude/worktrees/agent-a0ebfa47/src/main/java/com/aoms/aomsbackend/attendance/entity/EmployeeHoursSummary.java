package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@Table(name = "employee_hours_summary")
@IdClass(AttendancePeriodId.class)
public class EmployeeHoursSummary {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "year")
    private Integer year;

    @Id
    @Column(name = "month")
    private Integer month;

    @Column(name = "in_office_days")
    private Integer inOfficeDays;

    @Column(name = "total_hours_worked")
    private Double totalHoursWorked;

    @Column(name = "avg_daily_hours")
    private Double avgDailyHours;

    @Column(name = "hours_reached_days")
    private Integer hoursReachedDays;

    @Column(name = "hours_missed_days")
    private Integer hoursMissedDays;

    @Column(name = "longest_session_minutes")
    private Integer longestSessionMinutes;

    @Column(name = "shortest_session_minutes")
    private Integer shortestSessionMinutes;

    @Column(name = "min_presence_minutes")
    private Integer minPresenceMinutes;
}
