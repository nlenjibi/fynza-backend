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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Immutable
@Entity
@Table(name = "employee_punctuality_summary")
@IdClass(AttendancePeriodId.class)
public class EmployeePunctualitySummary {

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

    @Column(name = "on_time_days")
    private Integer onTimeDays;

    @Column(name = "late_days")
    private Integer lateDays;

    @Column(name = "on_time_rate_pct")
    private Double onTimeRatePct;

    @Column(name = "avg_minutes_late")
    private Double avgMinutesLate;

    @Column(name = "max_minutes_late")
    private Integer maxMinutesLate;

    @Column(name = "earliest_arrival")
    private LocalDateTime earliestArrival;

    @Column(name = "latest_arrival")
    private LocalDateTime latestArrival;
}
