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

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@Table(name = "employee_weekly_remote_usage_history")
@IdClass(WeekPeriodId.class)
public class EmployeeWeeklyRemoteUsageHistory {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "year")
    private Integer year;

    @Id
    @Column(name = "week_number")
    private Integer weekNumber;

    @Column(name = "week_start_date")
    private LocalDate weekStartDate;

    @Column(name = "week_end_date")
    private LocalDate weekEndDate;

    @Column(name = "remote_days_used")
    private Integer remoteDaysUsed;

    @Column(name = "weekly_limit")
    private Integer weeklyLimit;

    @Column(name = "over_limit")
    private Boolean overLimit;
}
