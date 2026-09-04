package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryResponse;
import com.aoms.aomsbackend.attendance.dto.HrAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import org.springframework.data.domain.Sort;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for HR-facing attendance queries scoped to a specific location.
 * Location access control is enforced upstream by {@code LocationRoleInterceptor}.
 */
public interface HrAttendanceService {

    /**
     * Returns paginated attendance records for all active employees at a location.
     * Supports optional filtering by department, manager subtree, single employee,
     * date range, and status.
     *
     * @param locationId   the building UUID — must be pre-authorised by the interceptor
     * @param department   optional department name filter
     * @param managerId    optional manager UUID; resolves full reporting subtree up to depth 5
     * @param employeeId   optional single employee UUID filter
     * @param fromDate     optional start date (inclusive)
     * @param toDate       optional end date (inclusive)
     * @param statuses     optional list of statuses to include
     * @param page         zero-based page index
     * @param size         page size
     * @param order        sort direction on record date
     * @return paginated records enriched with employee name, department, and work session data
     */
    PaginatedResponse<HrAttendanceRecordResponse> getLocationAttendance(
            UUID locationId,
            String department,
            UUID managerId,
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            int page,
            int size,
            Sort.Direction order);

    /**
     * Returns aggregated attendance counts for a single building on a single date.
     * Computed via a single SQL {@code GROUP BY} — never in Java.
     *
     * @param locationId the building UUID
     * @param date       the date to aggregate
     * @return one count per status value; all counts are 0 when no records exist
     */
    AttendanceSummaryResponse getSummary(UUID locationId, LocalDate date);

    /**
     * Writes attendance records as CSV directly to the provided output stream.
     * Applies the same filters as the list endpoint. Date range must be pre-validated
     * against the 90-day cap before calling this method.
     *
     * @param locationId   the building UUID
     * @param department   optional department name filter
     * @param managerId    optional manager UUID; resolves full reporting subtree
     * @param employeeId   optional single employee UUID filter
     * @param fromDate     start date (inclusive, required)
     * @param toDate       end date (inclusive, required)
     * @param statuses     optional list of statuses to include
     * @param outputStream target stream for CSV content
     * @throws ExportWindowTooLargeException if caller has not pre-validated the date window
     */
    void streamHrAttendanceCsv(
            UUID locationId,
            String department,
            UUID managerId,
            UUID employeeId,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            OutputStream outputStream);
}
