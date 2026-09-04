package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.CalendarDayResponse;
import com.aoms.aomsbackend.attendance.dto.TeamAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import org.springframework.data.domain.Pageable;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for retrieving team attendance data scoped to a manager's direct reports.
 */
public interface TeamAttendanceService {
    /**
     * Returns paginated attendance records for the manager's direct reports.
     *
     * @param userId           authenticated user ID (resolved to a manager employee)
     * @param employeeIdFilter optional filter to a single direct report; returns 403 if not a direct report
     * @param fromDate         optional start date filter (inclusive)
     * @param toDate           optional end date filter (inclusive)
     * @param statuses         optional attendance status filter
     * @param pageable         pagination and sort parameters
     * @return paginated attendance records with employee name and work session details
     */
    PaginatedResponse<TeamAttendanceRecordResponse> getTeamAttendance(
            UUID userId,
            UUID employeeIdFilter,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            Pageable pageable);

    /**
     * Returns a calendar snapshot for a single date showing each direct report's attendance status.
     * Employees with no attendance record for the given date are included with {@code status: null}.
     *
     * @param userId authenticated user ID (resolved to a manager employee)
     * @param date   the calendar date to query
     * @return calendar response with one entry per direct report
     */
    CalendarDayResponse getTeamCalendar(UUID userId, LocalDate date);

    /**
     * Writes team attendance records as CSV directly to the provided output stream.
     * Enforces a maximum 90-day date window.
     *
     * @param userId           authenticated user ID (resolved to a manager employee)
     * @param fromDate         start date (inclusive, required)
     * @param toDate           end date (inclusive, required)
     * @param employeeIdFilter optional filter to a single direct report
     * @param statuses         optional attendance status filter
     * @param outputStream     target stream for the CSV content
     * @throws ExportWindowTooLargeException if the date range exceeds 90 days
     */
    void streamTeamAttendanceCsv(
            UUID userId,
            LocalDate fromDate,
            LocalDate toDate,
            UUID employeeIdFilter,
            List<AttendanceStatus> statuses,
            OutputStream outputStream);
}
