package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.AttendanceStampLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceStampLogRepository extends JpaRepository<AttendanceStampLog, UUID> {
    Optional<AttendanceStampLog> findByJobNameAndLocationIdAndTargetDate(String jobName, UUID locationId, LocalDate targetDate);
}