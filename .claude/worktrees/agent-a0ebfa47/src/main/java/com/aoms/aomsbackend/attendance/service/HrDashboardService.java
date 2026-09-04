package com.aoms.aomsbackend.attendance.service;

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
import com.aoms.aomsbackend.common.responses.PaginatedResponse;

import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface HrDashboardService {

    // ── Location tab ──────────────────────────────────────────────────────────
    List<LocationDailySummaryDto> getLocationDailySummary(UUID locationId, LocalDate fromDate, LocalDate toDate);
    List<LocationDowChartDto> getLocationDowChart(UUID locationId);
    List<LocationTrendDto> getLocationTrend(UUID locationId, LocalDate fromDate, LocalDate toDate);
    List<LocationLatenessSummaryDto> getLocationLatenessSummary(UUID locationId, LocalDate fromDate, LocalDate toDate);

    // ── Org tab ───────────────────────────────────────────────────────────────
    List<OrgDailySummaryDto> getOrgDailySummary(UUID orgId, LocalDate fromDate, LocalDate toDate);
    List<OrgDepartmentSummaryDto> getOrgDepartmentSummary(UUID orgId, LocalDate fromDate, LocalDate toDate);
    PaginatedResponse<OrgEmployeeAttendanceDto> getOrgEmployeeAttendance(UUID orgId, LocalDate fromDate, LocalDate toDate, int page, int size);
    List<OrgLocationComparisonDto> getOrgLocationComparison(UUID orgId, LocalDate fromDate, LocalDate toDate);

    // ── Analytics tab ─────────────────────────────────────────────────────────
    AttendanceSummaryDto getAttendanceDailySummary(UUID locationId, LocalDate date);
    PaginatedResponse<AttendanceDetailDto> getAttendanceDetail(UUID locationId, LocalDate fromDate, LocalDate toDate, int page, int size);
    void streamAttendanceExportCsv(UUID locationId, LocalDate fromDate, LocalDate toDate, OutputStream outputStream);
    List<ChronicAbsenteeismDto> getChronicAbsenteeism(UUID locationId, LocalDate fromDate, LocalDate toDate);
    List<RemoteUsageSummaryDto> getRemoteUsageSummary(UUID locationId, LocalDate fromDate, LocalDate toDate);
    List<AttendanceOverrideAuditDto> getOverrideAudit(UUID locationId, LocalDate fromDate, LocalDate toDate);
}
