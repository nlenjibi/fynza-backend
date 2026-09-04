package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.AttendancePeriodId;
import com.aoms.aomsbackend.attendance.entity.EmployeePunctualitySummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeePunctualitySummaryRepository
        extends JpaRepository<EmployeePunctualitySummary, AttendancePeriodId> {

    List<EmployeePunctualitySummary> findByUserIdOrderByYearDescMonthDesc(UUID userId);

    List<EmployeePunctualitySummary> findByUserIdAndYearOrderByMonthAsc(UUID userId, Integer year);

    Optional<EmployeePunctualitySummary> findByUserIdAndYearAndMonth(
            UUID userId, Integer year, Integer month);
}
