package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.CalendarDayResponse;
import com.aoms.aomsbackend.attendance.dto.TeamAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.service.TeamAttendanceService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.config.util.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for manager-facing team attendance views.
 *
 * <p>All endpoints require {@code MANAGER} role at the organization specified
 * by the {@code X-Organization-Id} header. Data is scoped to the manager's
 * direct reports at the same primary building.</p>
 */
@RestController
@RequestMapping("/api/v1/attendance/team")
@RequiresRole(UserRoleType.MANAGER)
@Tag(name = "Team Attendance", description = "Manager endpoints for viewing team attendance")
@RequiredArgsConstructor
public class TeamAttendanceController {
    private static final long MAX_EXPORT_DAYS = 90;
    private final TeamAttendanceService teamAttendanceService;
    /**
     * Returns paginated attendance records for the authenticated manager's direct reports.
     * Supports optional filtering by employee, date range, and attendance status.
     *
     * @param employeeId optional UUID to filter to a single direct report (403 if not a direct report)
     * @param fromDate   optional start date filter (inclusive)
     * @param toDate     optional end date filter (inclusive)
     * @param status     optional list of attendance statuses to include
     * @param page       zero-based page index (default 0)
     * @param size       page size (default 20)
     * @param order      sort direction on record date (default DESC)
     * @return paginated attendance records wrapped in {@link ResponseWrapper}
     */
    @GetMapping
    @Operation(summary = "Get paginated team attendance records",
            description = "Returns attendance records for the manager's direct reports at their location",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Attendance records returned"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role or employee is not a direct report"),
                    @ApiResponse(responseCode = "404", description = "Manager employee record not found")
            })
    public ResponseEntity<ResponseWrapper<PaginatedResponse<TeamAttendanceRecordResponse>>> getTeamAttendance(
            @Parameter(description = "Filter to a single direct report by employee ID")
            @RequestParam(required = false) UUID employeeId,
            @Parameter(description = "Start date filter (inclusive, yyyy-MM-dd)")
            @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "End date filter (inclusive, yyyy-MM-dd)")
            @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "Filter by attendance status(es)")
            @RequestParam(required = false) List<AttendanceStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") Sort.Direction order) {

        UUID userId = SessionUtils.extractUserId();
        var pageable = PageRequest.of(page, size, Sort.by(order, "recordDate"));

        PaginatedResponse<TeamAttendanceRecordResponse> result = teamAttendanceService.getTeamAttendance(userId, employeeId, fromDate, toDate, status, pageable);

        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    /**
     * Returns a calendar snapshot for a single date showing each direct report's
     * attendance status. Employees with no record for the date are included with
     * {@code status: null}.
     *
     * @param date the calendar date to query (yyyy-MM-dd)
     * @return calendar response with one entry per direct report
     */
    @GetMapping("/calendar")
    @Operation(summary = "Get calendar snapshot for a date",
            description = "Returns one entry per direct report for the given date; employees with no record have status null",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Calendar snapshot returned"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role or location access"),
                    @ApiResponse(responseCode = "404", description = "Manager employee record not found")
            })
    public ResponseEntity<ResponseWrapper<CalendarDayResponse>> getTeamCalendar(
            @Parameter(description = "Calendar date (yyyy-MM-dd)", required = true)
            @RequestParam LocalDate date
    ) {
        UUID userId = SessionUtils.extractUserId();
        CalendarDayResponse result = teamAttendanceService.getTeamCalendar(userId, date);

        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    /**
     * Streams team attendance records as a CSV file download.
     * Enforces a maximum 90-day date window before streaming begins.
     *
     * @param fromDate   start date (inclusive, required)
     * @param toDate     end date (inclusive, required)
     * @param employeeId optional filter to a single direct report
     * @param status     optional list of attendance statuses to include
     * @return streaming CSV response with Content-Disposition attachment header
     * @throws ExportWindowTooLargeException if the date range exceeds 90 days
     */
    @GetMapping("/export")
    @Operation(summary = "Export team attendance as CSV",
            description = "Streams a CSV file of attendance records. Maximum 90-day date window.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV file streamed"),
                    @ApiResponse(responseCode = "400", description = "Export window exceeds 90 days"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role or employee is not a direct report"),
                    @ApiResponse(responseCode = "404", description = "Manager employee record not found")
            })
    public ResponseEntity<StreamingResponseBody> exportTeamAttendance(
            @Parameter(description = "Start date (inclusive, yyyy-MM-dd)", required = true)
            @RequestParam LocalDate fromDate,
            @Parameter(description = "End date (inclusive, yyyy-MM-dd)", required = true)
            @RequestParam LocalDate toDate,
            @Parameter(description = "Filter to a single direct report by employee ID")
            @RequestParam(required = false) UUID employeeId,
            @Parameter(description = "Filter by attendance status(es)")
            @RequestParam(required = false) List<AttendanceStatus> status
    ) {
        UUID userId = SessionUtils.extractUserId();

        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_EXPORT_DAYS) {
            throw new ExportWindowTooLargeException();
        }

        String filename = "team-attendance-" + fromDate + "-" + toDate + ".csv";

        StreamingResponseBody body = outputStream -> teamAttendanceService.streamTeamAttendanceCsv(userId, fromDate, toDate, employeeId, status, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
