package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.dto.AttendanceDetailDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceExportDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceOverrideAuditDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryDto;
import com.aoms.aomsbackend.attendance.dto.ChronicAbsenteeismDto;
import com.aoms.aomsbackend.attendance.dto.LocationDailySummaryDto;
import com.aoms.aomsbackend.attendance.dto.LocationDowChartDto;
import com.aoms.aomsbackend.attendance.dto.LocationLatenessSummaryDto;
import com.aoms.aomsbackend.attendance.dto.LocationTrendDto;
import com.aoms.aomsbackend.attendance.dto.OrgDailySummaryDto;
import com.aoms.aomsbackend.attendance.dto.OrgDepartmentSummaryDto;
import com.aoms.aomsbackend.attendance.dto.OrgEmployeeAttendanceDto;
import com.aoms.aomsbackend.attendance.dto.OrgLocationComparisonDto;
import com.aoms.aomsbackend.attendance.dto.RemoteUsageSummaryDto;
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

public interface HrDashboardRepository extends JpaRepository<AttendanceRecord, UUID> {

    // ── Location views ────────────────────────────────────────────────────────

    @Query(nativeQuery = true, value = """
            SELECT building_id, building_name, record_date, department,
                   total_employees_with_record, in_office_count, present_count, late_count,
                   insufficient_hours_count, remote_count, on_leave_count, absent_count,
                   attendance_rate_pct
            FROM hr_location_daily_summary
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY record_date
            """)
    List<LocationDailySummaryDto> findLocationDailySummary(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true, value = """
            SELECT building_id, department, year, month, week_number,
                   day_of_week, day_of_week_short, day_order,
                   in_office_count, remote_count, on_leave_count, absent_count, total_count
            FROM hr_location_dow_chart
            WHERE building_id = :buildingId
            ORDER BY day_order
            """)
    List<LocationDowChartDto> findLocationDowChart(
            @Param("buildingId") UUID buildingId);

    @Query(nativeQuery = true, value = """
            SELECT building_id, building_name, record_date, year, month, week_number,
                   total_employees_with_record, in_office_count, remote_count,
                   on_leave_count, absent_count, attendance_rate_pct
            FROM hr_location_trend
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY record_date
            """)
    List<LocationTrendDto> findLocationTrend(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true, value = """
            SELECT building_id, employee_id, employee_name, employee_code, department,
                   manager_id, year, month, late_days, avg_minutes_late, max_minutes_late,
                   total_days_with_record, late_rate_pct
            FROM hr_location_lateness_summary
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY late_rate_pct DESC
            """)
    List<LocationLatenessSummaryDto> findLocationLatenessSummary(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ── Org views ─────────────────────────────────────────────────────────────

    @Query(nativeQuery = true, value = """
            SELECT organisation_id, country_name, building_id, building_name, office_id, city_name,
                   record_date, total_employees_with_record, in_office_count, present_count, late_count,
                   insufficient_hours_count, remote_count, on_leave_count, absent_count, attendance_rate_pct
            FROM hr_org_daily_summary
            WHERE organisation_id = :orgId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY record_date
            """)
    List<OrgDailySummaryDto> findOrgDailySummary(
            @Param("orgId") UUID orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true, value = """
            SELECT organisation_id, building_id, building_name, record_date, department,
                   total_employees_with_record, in_office_count, present_count, late_count,
                   remote_count, on_leave_count, absent_count, attendance_rate_pct
            FROM hr_org_department_summary
            WHERE organisation_id = :orgId
              AND (:fromDate IS NULL OR record_date >= :fromDate)
              AND (:toDate IS NULL OR record_date <= :toDate)
            ORDER BY total_employees_with_record DESC
            """)
    List<OrgDepartmentSummaryDto> findOrgDepartmentSummary(
            @Param("orgId") UUID orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true,
            value = """
            SELECT organisation_id, building_id, building_name, city_name, record_id,
                   record_date, status, is_overridden, override_reason,
                   employee_id, employee_full_name, employee_code, department, job_title, team,
                   first_badge_in, last_badge_out, total_duration_minutes, is_late, minutes_late
            FROM hr_org_employee_attendance
            WHERE organisation_id = :orgId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY record_date DESC, employee_full_name
            """,
            countQuery = """
            SELECT COUNT(*) FROM hr_org_employee_attendance
            WHERE organisation_id = :orgId
              AND record_date BETWEEN :fromDate AND :toDate
            """)
    Page<OrgEmployeeAttendanceDto> findOrgEmployeeAttendance(
            @Param("orgId") UUID orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @Query(nativeQuery = true, value = """
            SELECT organisation_id, building_id, building_name, city_name, record_date,
                   year, month, total_employees_with_record, in_office_count, remote_count,
                   on_leave_count, absent_count, attendance_rate_pct
            FROM hr_org_location_comparison
            WHERE organisation_id = :orgId
              AND (:fromDate IS NULL OR record_date >= :fromDate)
              AND (:toDate IS NULL OR record_date <= :toDate)
            ORDER BY attendance_rate_pct DESC
            """)
    List<OrgLocationComparisonDto> findOrgLocationComparison(
            @Param("orgId") UUID orgId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    // ── Analytics views ───────────────────────────────────────────────────────

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
    AttendanceSummaryDto findAttendanceDailySummary(
            @Param("buildingId") UUID buildingId,
            @Param("date") LocalDate date);

    @Query(nativeQuery = true,
            value = """
            SELECT employee_id, employee_full_name, employee_code, department, job_title, team,
                   building_id, record_date, status, first_badge_in, last_badge_out,
                   total_duration_minutes, is_late, minutes_late, is_overridden, override_reason
            FROM hr_attendance_detail_view
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY record_date DESC, employee_full_name
            """,
            countQuery = """
            SELECT COUNT(*) FROM hr_attendance_detail_view
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            """)
    Page<AttendanceDetailDto> findAttendanceDetail(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable);

    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "50"))
    @Query(nativeQuery = true, value = """
            SELECT employee_id, employee_name, employee_code, department, team, rank,
                   building_id, record_date, status, first_badge_in_local, last_badge_out_local,
                   total_duration_minutes, minutes_late, is_late, is_overridden
            FROM hr_attendance_export_view
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY record_date, employee_name
            """)
    Stream<AttendanceExportDto> streamAttendanceExport(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true, value = """
            SELECT organisation_id, building_id, employee_id, employee_name, employee_code,
                   department, manager_id, year, month, absent_days, in_office_days,
                   total_days_with_record, absence_rate_pct
            FROM hr_chronic_absenteeism
            WHERE building_id = :buildingId
              AND (:fromDate IS NULL OR from_date >= :fromDate)
              AND (:toDate IS NULL OR to_date <= :toDate)
            ORDER BY absence_rate_pct DESC
            """)
    List<ChronicAbsenteeismDto> findChronicAbsenteeism(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true, value = """
            SELECT organisation_id, building_id, employee_id, employee_name, employee_code,
                   department, manager_id, year, month, remote_days, in_office_days,
                   absent_days, leave_days, total_days_with_record, remote_usage_pct
            FROM hr_remote_usage_summary
            WHERE building_id = :buildingId
              AND (:fromDate IS NULL OR from_date >= :fromDate)
              AND (:toDate IS NULL OR to_date <= :toDate)
            ORDER BY remote_usage_pct DESC
            """)
    List<RemoteUsageSummaryDto> findRemoteUsageSummary(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);

    @Query(nativeQuery = true, value = """
            SELECT organisation_id, building_id, building_name, record_id, record_date, year, month,
                   employee_id, employee_name, employee_code, department,
                   original_status, current_status, override_reason,
                   overridden_at, override_by, overridden_by_name, overrider_job_title
            FROM hr_attendance_override_audit
            WHERE building_id = :buildingId
              AND record_date BETWEEN :fromDate AND :toDate
            ORDER BY overridden_at DESC
            """)
    List<AttendanceOverrideAuditDto> findOverrideAudit(
            @Param("buildingId") UUID buildingId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
