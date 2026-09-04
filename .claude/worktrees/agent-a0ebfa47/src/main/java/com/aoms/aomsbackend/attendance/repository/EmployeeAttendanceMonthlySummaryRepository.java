package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.AttendancePeriodId;
import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceMonthlySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAttendanceMonthlySummaryRepository
        extends JpaRepository<EmployeeAttendanceMonthlySummary, AttendancePeriodId> {

    List<EmployeeAttendanceMonthlySummary> findByUserIdOrderByYearDescMonthDesc(UUID userId);

    List<EmployeeAttendanceMonthlySummary> findByUserIdAndYearOrderByMonthAsc(UUID userId, Integer year);

    Optional<EmployeeAttendanceMonthlySummary> findByUserIdAndYearAndMonth(
            UUID userId, Integer year, Integer month);
}
