package com.aoms.aomsbackend.attendance.repository;

import com.aoms.aomsbackend.attendance.entity.AttendanceRecord;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.persistence.QueryHint;
import org.hibernate.jpa.HibernateHints;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.QueryHints;


/**
 * Repository for the read-only {@link AttendanceRecord} table managed by the data-engineering pipeline.
 * All queries eagerly fetch the associated {@link com.aoms.aomsbackend.attendance.entity.WorkSession}
 * via LEFT JOIN FETCH to avoid N+1 queries.
 */
public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, UUID> {
    /**
     * Returns a paginated set of attendance records for the given user IDs with optional
     * date range and status filters. Eagerly fetches the associated work session.
     *
     * @param userIds  employee IDs to include (typically a manager's direct reports)
     * @param fromDate optional start date (inclusive); null to skip
     * @param toDate   optional end date (inclusive); null to skip
     * @param statuses optional status filter; null to include all
     * @param pageable pagination and sort specification
     * @return page of matching attendance records with work session data
     */
    @Query(value = "SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.workSession"
            + " WHERE ar.userId IN :userIds"
            + " AND (:fromDate IS NULL OR ar.recordDate >= :fromDate)"
            + " AND (:toDate IS NULL OR ar.recordDate <= :toDate)"
            + " AND (:statuses IS NULL OR ar.status IN :statuses)",
            countQuery = "SELECT COUNT(ar) FROM AttendanceRecord ar"
                    + " WHERE ar.userId IN :userIds"
                    + " AND (:fromDate IS NULL OR ar.recordDate >= :fromDate)"
                    + " AND (:toDate IS NULL OR ar.recordDate <= :toDate)"
                    + " AND (:statuses IS NULL OR ar.status IN :statuses)")
    Page<AttendanceRecord> findTeamAttendance(
            @Param("userIds") List<UUID> userIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<AttendanceStatus> statuses,
            Pageable pageable);

    /**
     * Returns attendance records for the given users on a specific date.
     * Used by the calendar snapshot endpoint.
     *
     * @param userIds employee IDs to include
     * @param date    the single calendar date to query
     * @return list of matching records (may be fewer than userIds if some have no record)
     */
    @Query("SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.workSession"
            + " WHERE ar.userId IN :userIds AND ar.recordDate = :date")
    List<AttendanceRecord> findByUserIdsAndDate(
            @Param("userIds") List<UUID> userIds,
            @Param("date") LocalDate date);

    /**
     * Streams attendance records matching the given filters ordered by date then user.
     * Uses a server-side cursor (HINT_FETCH_SIZE) so rows are never fully buffered in memory.
     * Must be consumed inside an active transaction and closed after use.
     *
     * @param userIds  employee IDs to include
     * @param fromDate start date (inclusive, required)
     * @param toDate   end date (inclusive, required)
     * @param statuses optional status filter; null to include all
     * @return lazy stream of matching records with work session data
     */
    @QueryHints(@QueryHint(name = HibernateHints.HINT_FETCH_SIZE, value = "50"))
    @Query("SELECT ar FROM AttendanceRecord ar LEFT JOIN FETCH ar.workSession"
            + " WHERE ar.userId IN :userIds"
            + " AND ar.recordDate >= :fromDate"
            + " AND ar.recordDate <= :toDate"
            + " AND (:statuses IS NULL OR ar.status IN :statuses)"
            + " ORDER BY ar.recordDate, ar.userId")
    Stream<AttendanceRecord> streamTeamAttendanceList(
            @Param("userIds") List<UUID> userIds,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("statuses") List<AttendanceStatus> statuses);

    Optional<AttendanceRecord> findByUserIdAndRecordDate(UUID userId, LocalDate recordDate);

    @Query("SELECT ar FROM AttendanceRecord ar WHERE ar.buildingId = :buildingId AND ar.recordDate = :date")
    List<AttendanceRecord> findByBuildingIdAndRecordDate(@Param("buildingId") UUID buildingId, @Param("date") LocalDate date);
}
