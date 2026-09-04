package com.aoms.aomsbackend.attendance.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeWeeklyRemoteUsageRepository extends JpaRepository<EmployeeWeeklyRemoteUsage, UUID> {

    Optional<EmployeeWeeklyRemoteUsage> findByUserId(UUID userId);
}
