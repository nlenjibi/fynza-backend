package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.WeekPeriodId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmployeeWeeklyRemoteUsageHistoryRepository
        extends JpaRepository<EmployeeWeeklyRemoteUsageHistory, WeekPeriodId> {

    List<EmployeeWeeklyRemoteUsageHistory> findByUserIdAndYearOrderByWeekNumberDesc(
            UUID userId, Integer year);
}
