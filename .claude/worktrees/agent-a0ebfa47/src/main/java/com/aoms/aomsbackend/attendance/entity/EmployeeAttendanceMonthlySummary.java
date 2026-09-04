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
@Table(name = "employee_attendance_monthly_summary")
@IdClass(AttendancePeriodId.class)
public class EmployeeAttendanceMonthlySummary {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Id
    @Column(name = "year")
    private Integer year;

    @Id
    @Column(name = "month")
    private Integer month;

    @Column(name = "days_with_record")
    private Integer daysWithRecord;

    @Column(name = "in_office_days")
    private Integer inOfficeDays;

    @Column(name = "present_days")
    private Integer presentDays;

    @Column(name = "late_days")
    private Integer lateDays;

    @Column(name = "insufficient_hours_days")
    private Integer insufficientHoursDays;

    @Column(name = "remote_days")
    private Integer remoteDays;

    @Column(name = "absent_days")
    private Integer absentDays;

    @Column(name = "on_leave_days")
    private Integer onLeaveDays;

    @Column(name = "public_holiday_days")
    private Integer publicHolidayDays;

    @Column(name = "total_hours_worked")
    private Double totalHoursWorked;

    @Column(name = "avg_daily_hours")
    private Double avgDailyHours;

    @Column(name = "hours_reached_days")
    private Integer hoursReachedDays;

    @Column(name = "attendance_rate_pct")
    private Double attendanceRatePct;
}
