package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryResponse;
import com.aoms.aomsbackend.attendance.dto.HrAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.attendance.service.HrAttendanceService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for HR-facing attendance views scoped to a specific location.
 *
 * <p>All endpoints require {@code HR} role. Location access is enforced by
 * {@code LocationRoleInterceptor} using the {@code locationId} path variable — HR users
 * can only access locations present in their authorized building set.</p>
 */
@RestController
@RequestMapping("/api/v1/attendance/location")
@RequiresRole(UserRoleType.HR)
@Tag(name = "HR Attendance", description = "HR endpoints for viewing attendance across a location")
@RequiredArgsConstructor
public class HrAttendanceController {

    private static final long MAX_EXPORT_DAYS = 90;

    private final HrAttendanceService hrAttendanceService;

    /**
     * Returns paginated attendance records for all active employees at a location.
     * Optional filters narrow the result by department, manager subtree, single employee,
     * date range, and status.
     *
     * @param locationId   building UUID derived from the path
     * @param department   optional department name filter
     * @param managerId    optional manager UUID; resolves full reporting subtree (max depth 5)
     * @param employeeId   optional filter to a single employee
     * @param fromDate     optional start date (inclusive, yyyy-MM-dd)
     * @param toDate       optional end date (inclusive, yyyy-MM-dd)
     * @param status       optional list of attendance statuses
     * @param page         zero-based page index (default 0)
     * @param size         page size (default 20)
     * @param order        sort direction on record date (default DESC)
     * @return paginated attendance records wrapped in {@link ResponseWrapper}
     */
    @GetMapping("/{locationId}")
    @Operation(
            summary = "Get paginated attendance records for a location",
            description = "Returns attendance records for all active employees at the location. Supports filtering by department, manager subtree, employee, date range, and status.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Attendance records returned"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role or location access denied"),
            }
    )
    public ResponseEntity<ResponseWrapper<PaginatedResponse<HrAttendanceRecordResponse>>> getLocationAttendance(
            @PathVariable UUID locationId,
            @Parameter(description = "Filter by department name") @RequestParam(required = false) String department,
            @Parameter(description = "Filter by manager subtree (resolves up to depth 5)") @RequestParam(required = false) UUID managerId,
            @Parameter(description = "Filter to a single employee") @RequestParam(required = false) UUID employeeId,
            @Parameter(description = "Start date filter (inclusive, yyyy-MM-dd)") @RequestParam(required = false) LocalDate fromDate,
            @Parameter(description = "End date filter (inclusive, yyyy-MM-dd)") @RequestParam(required = false) LocalDate toDate,
            @Parameter(description = "Filter by attendance status(es)") @RequestParam(required = false) List<AttendanceStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "DESC") Sort.Direction order
    ) {
        PaginatedResponse<HrAttendanceRecordResponse> result = hrAttendanceService.getLocationAttendance(
                locationId, department, managerId, employeeId, fromDate, toDate, status, page, size, order);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    /**
     * Returns aggregated attendance counts for a location on a single date.
     * Counts are computed via a single SQL aggregation — no per-record iteration in Java.
     *
     * @param locationId building UUID derived from the path
     * @param date       the date to summarise (yyyy-MM-dd)
     * @return one count per attendance status value
     */
    @GetMapping("/{locationId}/summary")
    @Operation(
            summary = "Get daily attendance summary for a location",
            description = "Returns total counts per status for a single date. Computed via a single SQL GROUP BY — no N+1.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Summary returned"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role or location access denied"),
            }
    )
    public ResponseEntity<ResponseWrapper<AttendanceSummaryResponse>> getSummary(
            @PathVariable UUID locationId,
            @Parameter(description = "Date to summarise (yyyy-MM-dd)", required = true) @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(ResponseWrapper.success(hrAttendanceService.getSummary(locationId, date)));
    }

    /**
     * Streams attendance records as a CSV file download.
     * The same filters as the list endpoint apply. Enforces a 90-day date window limit.
     *
     * @param locationId building UUID derived from the path
     * @param department optional department name filter
     * @param managerId  optional manager UUID; resolves full reporting subtree
     * @param employeeId optional filter to a single employee
     * @param fromDate   start date (inclusive, required, yyyy-MM-dd)
     * @param toDate     end date (inclusive, required, yyyy-MM-dd)
     * @param status     optional list of attendance statuses
     * @return streaming CSV response with Content-Disposition attachment header
     * @throws ExportWindowTooLargeException if the date range exceeds 90 days
     */
    @GetMapping("/{locationId}/export")
    @Operation(
            summary = "Export location attendance as CSV",
            description = "Streams a CSV file of attendance records. Maximum 90-day date window. No in-memory buffer.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV file streamed"),
                    @ApiResponse(responseCode = "400", description = "Export window exceeds 90 days"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role or location access denied"),
            }
    )
    public ResponseEntity<StreamingResponseBody> exportCsv(
            @PathVariable UUID locationId,
            @Parameter(description = "Filter by department name") @RequestParam(required = false) String department,
            @Parameter(description = "Filter by manager subtree (resolves up to depth 5)") @RequestParam(required = false) UUID managerId,
            @Parameter(description = "Filter to a single employee") @RequestParam(required = false) UUID employeeId,
            @Parameter(description = "Start date (inclusive, yyyy-MM-dd)", required = true) @RequestParam LocalDate fromDate,
            @Parameter(description = "End date (inclusive, yyyy-MM-dd)", required = true) @RequestParam LocalDate toDate,
            @Parameter(description = "Filter by attendance status(es)") @RequestParam(required = false) List<AttendanceStatus> status
    ) {
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_EXPORT_DAYS) {
            throw new ExportWindowTooLargeException();
        }

        String filename = "attendance-" + locationId + "-" + fromDate + "-" + toDate + ".csv";
        StreamingResponseBody body = outputStream ->
                hrAttendanceService.streamHrAttendanceCsv(
                        locationId, department, managerId, employeeId, fromDate, toDate, status, outputStream);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
