package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceSelfView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeAttendanceSelfViewRepository extends JpaRepository<EmployeeAttendanceSelfView, UUID> {

    Page<EmployeeAttendanceSelfView> findByUserIdAndRecordDateBetween(
            UUID userId, LocalDate fromDate, LocalDate toDate, Pageable pageable);

    Page<EmployeeAttendanceSelfView> findByUserIdAndRecordDateBetweenAndStatusIn(
            UUID userId, LocalDate fromDate, LocalDate toDate, List<String> statuses, Pageable pageable);

    @Query(value = "SELECT * FROM employee_attendance_self_view WHERE record_id = :id AND user_id = :userId",
           nativeQuery = true)
    Optional<EmployeeAttendanceSelfView> findByIdAndUserIdNative(
            @Param("id") UUID id, @Param("userId") UUID userId);

    Page<EmployeeAttendanceSelfView> findByUserIdAndRecordDateBetweenAndIsLate(
            UUID userId, LocalDate from, LocalDate to, Boolean isLate, Pageable pageable);

    Page<EmployeeAttendanceSelfView> findByUserIdAndRecordDateBetweenAndIsLateAndStatusIn(
            UUID userId, LocalDate from, LocalDate to, Boolean isLate, List<String> statuses, Pageable pageable);
}
