package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.AttendancePeriodId;
import com.aoms.aomsbackend.attendance.entity.EmployeeHoursSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeHoursSummaryRepository
        extends JpaRepository<EmployeeHoursSummary, AttendancePeriodId> {

    List<EmployeeHoursSummary> findByUserIdOrderByYearDescMonthDesc(UUID userId);

    List<EmployeeHoursSummary> findByUserIdAndYearOrderByMonthAsc(UUID userId, Integer year);

    Optional<EmployeeHoursSummary> findByUserIdAndYearAndMonth(
            UUID userId, Integer year, Integer month);
}
