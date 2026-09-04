package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.EmployeeTodayStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeTodayStatusRepository extends JpaRepository<EmployeeTodayStatus, UUID> {

    Optional<EmployeeTodayStatus> findByUserId(UUID userId);
}
