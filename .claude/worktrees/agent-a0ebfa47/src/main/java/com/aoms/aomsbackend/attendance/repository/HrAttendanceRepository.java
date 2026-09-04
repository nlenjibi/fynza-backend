package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.dto.AttendanceDetailDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceExportDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryDto;
import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Repository for HR-scoped attendance queries. All queries target views pre-built by the
 * data-engineering pipeline and are bounded by {@code buildingId} to enforce the location
 * access boundary established by {@code LocationRoleInterceptor}.
 */
public interface HrAttendanceRepository extends JpaRepository<AttendanceRecord, UUID> {

    @Query(nativeQuery = true,
            value = """
            SELECT employee_id, employee_full_name, employee_code, department, job_title, team,
                   building_id, record_date, status, first_badge_in, last_badge_out,
                   total_duration_minutes, is_late, minutes_late, is_overridden, override_reason
            FROM hr_attendance_detail_view
            WHERE building_id = :buildingId
              AND employee_id IN :userIds
              AND (:fromDate IS NULL OR record_date >= CAST(:fromDate AS date))
              AND (:toDate IS NULL OR record_date <= CAST(:toDate AS date))
              AND status IN :statuses
            """,
            countQuery = """
            SELECT COUNT(*) FROM hr_attendance_detail_view
            WHERE building_id = :buildingId
              AND employee_id IN :userIds
              AND (:fromDate IS NULL OR record_date >= CAST(:fromDate AS date))
              AND (:toDate IS NULL OR record_date <= CAST(:toDate AS date))
              AND status IN :statuses
            """)
    Page<AttendanceDetailDto> findByLocationAndUsers(
            @Param("buildingId") UUID buildingId,
            @Param("userIds") List<UUID> userIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<String> statuses,
            Pageable pageable);

    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "50"))
    @Query(nativeQuery = true, value = """
            SELECT employee_id, employee_name, employee_code, department, team, rank,
                   building_id, record_date, status, first_badge_in_local, last_badge_out_local,
                   total_duration_minutes, minutes_late, is_late, is_overridden
            FROM hr_attendance_export_view
            WHERE building_id = :buildingId
              AND employee_id IN :userIds
              AND record_date >= CAST(:fromDate AS date)
              AND record_date <= CAST(:toDate AS date)
              AND status IN :statuses
            ORDER BY record_date, employee_name
            """)
    Stream<AttendanceExportDto> streamByLocationAndUsers(
            @Param("buildingId") UUID buildingId,
            @Param("userIds") List<UUID> userIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<String> statuses);

    @Query(nativeQuery = true, value = """
            SELECT
                CAST(:date AS date)                                                                AS date,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'PRESENT'),            0)    AS total_present,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'LATE'),               0)    AS total_late,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'ABSENT'),             0)    AS total_absent,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'REMOTE'),             0)    AS total_remote,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'ON_LEAVE'),           0)    AS total_on_leave,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'PUBLIC_HOLIDAY'),     0)    AS total_public_holiday,
                COALESCE(SUM(employee_count) FILTER (WHERE status = 'INSUFFICIENT_HOURS'), 0)    AS total_insufficient_hours
            FROM hr_attendance_daily_summary_view
            WHERE building_id = :buildingId AND record_date = CAST(:date AS date)
            """)
    AttendanceSummaryDto getSummary(
            @Param("buildingId") UUID buildingId,
            @Param("date") LocalDate date);
}
