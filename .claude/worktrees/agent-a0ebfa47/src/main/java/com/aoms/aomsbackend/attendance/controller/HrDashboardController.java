package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.AttendanceDetailDto;
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
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.attendance.service.HrDashboardService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/api/v1/hr-dashboard")
@RequiresRole(UserRoleType.HR)
@Tag(name = "HR Dashboard")
@RequiredArgsConstructor
public class HrDashboardController {

    private static final long MAX_EXPORT_DAYS = 90;

    private final HrDashboardService hrDashboardService;

    // ── Location tab ──────────────────────────────────────────────────────────

    @GetMapping("/location/{locationId}/daily-summary")
    @Operation(summary = "Daily attendance summary for a location")
    public ResponseEntity<ResponseWrapper<List<LocationDailySummaryDto>>> getLocationDailySummary(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getLocationDailySummary(locationId, fromDate, toDate)));
    }

    @GetMapping("/location/{locationId}/dow-chart")
    @Operation(summary = "Day-of-week attendance breakdown for a location")
    public ResponseEntity<ResponseWrapper<List<LocationDowChartDto>>> getLocationDowChart(
            @PathVariable UUID locationId) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getLocationDowChart(locationId)));
    }

    @GetMapping("/location/{locationId}/trend")
    @Operation(summary = "Attendance trend over time for a location")
    public ResponseEntity<ResponseWrapper<List<LocationTrendDto>>> getLocationTrend(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getLocationTrend(locationId, fromDate, toDate)));
    }

    @GetMapping("/location/{locationId}/lateness")
    @Operation(summary = "Lateness summary for a location")
    public ResponseEntity<ResponseWrapper<List<LocationLatenessSummaryDto>>> getLocationLatenessSummary(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getLocationLatenessSummary(locationId, fromDate, toDate)));
    }

    // ── Org tab ───────────────────────────────────────────────────────────────

    @GetMapping("/org/{organizationId}/daily-summary")
    @Operation(summary = "Daily attendance summary across the organisation")
    public ResponseEntity<ResponseWrapper<List<OrgDailySummaryDto>>> getOrgDailySummary(
            @PathVariable UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getOrgDailySummary(organizationId, fromDate, toDate)));
    }

    @GetMapping("/org/{organizationId}/department-summary")
    @Operation(summary = "Attendance breakdown by department")
    public ResponseEntity<ResponseWrapper<List<OrgDepartmentSummaryDto>>> getOrgDepartmentSummary(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getOrgDepartmentSummary(organizationId, fromDate, toDate)));
    }

    @GetMapping("/org/{organizationId}/employees")
    @Operation(summary = "Paginated employee attendance across the organisation")
    public ResponseEntity<ResponseWrapper<PaginatedResponse<OrgEmployeeAttendanceDto>>> getOrgEmployeeAttendance(
            @PathVariable UUID organizationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getOrgEmployeeAttendance(organizationId, fromDate, toDate, page, size)));
    }

    @GetMapping("/org/{organizationId}/location-comparison")
    @Operation(summary = "Attendance comparison across locations in the organisation")
    public ResponseEntity<ResponseWrapper<List<OrgLocationComparisonDto>>> getOrgLocationComparison(
            @PathVariable UUID organizationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getOrgLocationComparison(organizationId, fromDate, toDate)));
    }

    // ── Analytics tab ─────────────────────────────────────────────────────────

    @GetMapping("/analytics/{locationId}/daily-summary")
    @Operation(summary = "Daily attendance counts for a location on a single date")
    public ResponseEntity<ResponseWrapper<AttendanceSummaryDto>> getAttendanceDailySummary(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getAttendanceDailySummary(locationId, date)));
    }

    @GetMapping("/analytics/{locationId}/detail")
    @Operation(summary = "Paginated attendance detail records for a location")
    public ResponseEntity<ResponseWrapper<PaginatedResponse<AttendanceDetailDto>>> getAttendanceDetail(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getAttendanceDetail(locationId, fromDate, toDate, page, size)));
    }

    @GetMapping("/analytics/{locationId}/export")
    @Operation(summary = "Stream attendance records as CSV (max 90-day window)")
    public ResponseEntity<StreamingResponseBody> exportAttendanceCsv(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        if (ChronoUnit.DAYS.between(fromDate, toDate) > MAX_EXPORT_DAYS) {
            throw new ExportWindowTooLargeException();
        }
        String filename = "attendance-export-" + locationId + "-" + fromDate + "-" + toDate + ".csv";
        StreamingResponseBody body = out ->
                hrDashboardService.streamAttendanceExportCsv(locationId, fromDate, toDate, out);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv; charset=utf-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }

    @GetMapping("/analytics/{locationId}/chronic-absenteeism")
    @Operation(summary = "Employees with chronic absenteeism patterns")
    public ResponseEntity<ResponseWrapper<List<ChronicAbsenteeismDto>>> getChronicAbsenteeism(
            @PathVariable UUID locationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getChronicAbsenteeism(locationId, fromDate, toDate)));
    }

    @GetMapping("/analytics/{locationId}/remote-usage")
    @Operation(summary = "Remote work usage summary per employee")
    public ResponseEntity<ResponseWrapper<List<RemoteUsageSummaryDto>>> getRemoteUsageSummary(
            @PathVariable UUID locationId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getRemoteUsageSummary(locationId, fromDate, toDate)));
    }

    @GetMapping("/analytics/{locationId}/override-audit")
    @Operation(summary = "Audit trail of attendance overrides")
    public ResponseEntity<ResponseWrapper<List<AttendanceOverrideAuditDto>>> getOverrideAudit(
            @PathVariable UUID locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(ResponseWrapper.success(
                hrDashboardService.getOverrideAudit(locationId, fromDate, toDate)));
    }
}
