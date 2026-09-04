package com.aoms.aomsbackend.attendance.service.impl;

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
import com.aoms.aomsbackend.attendance.repository.HrDashboardRepository;
import com.aoms.aomsbackend.attendance.service.HrDashboardService;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HrDashboardServiceImpl implements HrDashboardService {

    private static final String EXPORT_CSV_HEADER =
            "Employee Name,Employee Code,Department,Team,Rank,Date,Status,First Badge In (Local),Last Badge Out (Local),Duration (mins),Minutes Late,Late,Overridden";

    private final HrDashboardRepository dashboardRepository;

    // ── Location tab ──────────────────────────────────────────────────────────

    @Override
    public List<LocationDailySummaryDto> getLocationDailySummary(UUID locationId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findLocationDailySummary(locationId, fromDate, toDate);
    }

    @Override
    public List<LocationDowChartDto> getLocationDowChart(UUID locationId) {
        return dashboardRepository.findLocationDowChart(locationId);
    }

    @Override
    public List<LocationTrendDto> getLocationTrend(UUID locationId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findLocationTrend(locationId, fromDate, toDate);
    }

    @Override
    public List<LocationLatenessSummaryDto> getLocationLatenessSummary(UUID locationId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findLocationLatenessSummary(locationId, fromDate, toDate);
    }

    // ── Org tab ───────────────────────────────────────────────────────────────

    @Override
    public List<OrgDailySummaryDto> getOrgDailySummary(UUID orgId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findOrgDailySummary(orgId, fromDate, toDate);
    }

    @Override
    public List<OrgDepartmentSummaryDto> getOrgDepartmentSummary(UUID orgId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findOrgDepartmentSummary(orgId, fromDate, toDate);
    }

    @Override
    public PaginatedResponse<OrgEmployeeAttendanceDto> getOrgEmployeeAttendance(
            UUID orgId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        return PaginatedResponse.from(
                dashboardRepository.findOrgEmployeeAttendance(orgId, fromDate, toDate, PageRequest.of(page, size)));
    }

    @Override
    public List<OrgLocationComparisonDto> getOrgLocationComparison(UUID orgId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findOrgLocationComparison(orgId, fromDate, toDate);
    }

    // ── Analytics tab ─────────────────────────────────────────────────────────

    @Override
    public AttendanceSummaryDto getAttendanceDailySummary(UUID locationId, LocalDate date) {
        return dashboardRepository.findAttendanceDailySummary(locationId, date);
    }

    @Override
    public PaginatedResponse<AttendanceDetailDto> getAttendanceDetail(
            UUID locationId, LocalDate fromDate, LocalDate toDate, int page, int size) {
        return PaginatedResponse.from(
                dashboardRepository.findAttendanceDetail(locationId, fromDate, toDate, PageRequest.of(page, size)));
    }

    @Override
    public void streamAttendanceExportCsv(UUID locationId, LocalDate fromDate, LocalDate toDate, OutputStream outputStream) {
        try (Stream<AttendanceExportDto> rows = dashboardRepository.streamAttendanceExport(locationId, fromDate, toDate);
             PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
            writer.println(EXPORT_CSV_HEADER);
            rows.forEach(row -> writeCsvRow(writer, row));
            writer.flush();
        }
    }

    @Override
    public List<ChronicAbsenteeismDto> getChronicAbsenteeism(UUID locationId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findChronicAbsenteeism(locationId, fromDate, toDate);
    }

    @Override
    public List<RemoteUsageSummaryDto> getRemoteUsageSummary(UUID locationId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findRemoteUsageSummary(locationId, fromDate, toDate);
    }

    @Override
    public List<AttendanceOverrideAuditDto> getOverrideAudit(UUID locationId, LocalDate fromDate, LocalDate toDate) {
        return dashboardRepository.findOverrideAudit(locationId, fromDate, toDate);
    }

    private void writeCsvRow(PrintWriter writer, AttendanceExportDto row) {
        writer.printf("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s%n",
                nullSafe(row.getEmployeeName()),
                nullSafe(row.getEmployeeCode()),
                nullSafe(row.getDepartment()),
                nullSafe(row.getTeam()),
                nullSafe(row.getRank()),
                row.getRecordDate(),
                row.getStatus(),
                row.getFirstBadgeInLocal() != null ? row.getFirstBadgeInLocal() : "",
                row.getLastBadgeOutLocal() != null ? row.getLastBadgeOutLocal() : "",
                nullSafe(row.getTotalDurationMinutes()),
                nullSafe(row.getMinutesLate()),
                Boolean.TRUE.equals(row.getIsLate()),
                Boolean.TRUE.equals(row.getIsOverridden()));
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "";
    }
}
