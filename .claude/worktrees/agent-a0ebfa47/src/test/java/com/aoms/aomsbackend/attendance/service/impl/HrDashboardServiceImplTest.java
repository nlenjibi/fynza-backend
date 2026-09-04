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
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrDashboardServiceImplTest {

    @Mock private HrDashboardRepository dashboardRepository;
    @InjectMocks private HrDashboardServiceImpl service;

    private static final UUID      LOCATION_ID = UUID.randomUUID();
    private static final UUID      ORG_ID      = UUID.randomUUID();
    private static final LocalDate FROM        = LocalDate.of(2026, 4, 1);
    private static final LocalDate TO          = LocalDate.of(2026, 4, 30);
    private static final LocalDate DATE        = LocalDate.of(2026, 4, 15);

    // ── Location: daily summary ───────────────────────────────────────────────

    @Test
    void getLocationDailySummary_returnsRepositoryResult() {
        LocationDailySummaryDto row = mock(LocationDailySummaryDto.class);
        when(dashboardRepository.findLocationDailySummary(LOCATION_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getLocationDailySummary(LOCATION_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findLocationDailySummary(LOCATION_ID, FROM, TO);
    }

    @Test
    void getLocationDailySummary_emptyResult_returnsEmptyList() {
        when(dashboardRepository.findLocationDailySummary(LOCATION_ID, FROM, TO)).thenReturn(List.of());

        assertThat(service.getLocationDailySummary(LOCATION_ID, FROM, TO)).isEmpty();
    }

    // ── Location: DOW chart ───────────────────────────────────────────────────

    @Test
    void getLocationDowChart_returnsRepositoryResult() {
        LocationDowChartDto row = mock(LocationDowChartDto.class);
        when(dashboardRepository.findLocationDowChart(LOCATION_ID)).thenReturn(List.of(row));

        assertThat(service.getLocationDowChart(LOCATION_ID)).containsExactly(row);
        verify(dashboardRepository).findLocationDowChart(LOCATION_ID);
    }

    // ── Location: trend ───────────────────────────────────────────────────────

    @Test
    void getLocationTrend_returnsRepositoryResult() {
        LocationTrendDto row = mock(LocationTrendDto.class);
        when(dashboardRepository.findLocationTrend(LOCATION_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getLocationTrend(LOCATION_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findLocationTrend(LOCATION_ID, FROM, TO);
    }

    @Test
    void getLocationTrend_emptyResult_returnsEmptyList() {
        when(dashboardRepository.findLocationTrend(LOCATION_ID, FROM, TO)).thenReturn(List.of());

        assertThat(service.getLocationTrend(LOCATION_ID, FROM, TO)).isEmpty();
    }

    // ── Location: lateness summary ────────────────────────────────────────────

    @Test
    void getLocationLatenessSummary_returnsRepositoryResult() {
        LocationLatenessSummaryDto row = mock(LocationLatenessSummaryDto.class);
        when(dashboardRepository.findLocationLatenessSummary(LOCATION_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getLocationLatenessSummary(LOCATION_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findLocationLatenessSummary(LOCATION_ID, FROM, TO);
    }

    // ── Org: daily summary ────────────────────────────────────────────────────

    @Test
    void getOrgDailySummary_returnsRepositoryResult() {
        OrgDailySummaryDto row = mock(OrgDailySummaryDto.class);
        when(dashboardRepository.findOrgDailySummary(ORG_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getOrgDailySummary(ORG_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findOrgDailySummary(ORG_ID, FROM, TO);
    }

    @Test
    void getOrgDailySummary_emptyResult_returnsEmptyList() {
        when(dashboardRepository.findOrgDailySummary(ORG_ID, FROM, TO)).thenReturn(List.of());

        assertThat(service.getOrgDailySummary(ORG_ID, FROM, TO)).isEmpty();
    }

    // ── Org: department summary ───────────────────────────────────────────────

    @Test
    void getOrgDepartmentSummary_returnsRepositoryResult() {
        OrgDepartmentSummaryDto row = mock(OrgDepartmentSummaryDto.class);
        when(dashboardRepository.findOrgDepartmentSummary(ORG_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getOrgDepartmentSummary(ORG_ID, FROM, TO)).containsExactly(row);
    }

    @Test
    void getOrgDepartmentSummary_nullDates_delegatesNullsToRepository() {
        when(dashboardRepository.findOrgDepartmentSummary(ORG_ID, null, null)).thenReturn(List.of());

        service.getOrgDepartmentSummary(ORG_ID, null, null);

        verify(dashboardRepository).findOrgDepartmentSummary(ORG_ID, null, null);
    }

    // ── Org: employee attendance (paginated) ──────────────────────────────────

    @Test
    void getOrgEmployeeAttendance_wrapsPageInPaginatedResponse() {
        OrgEmployeeAttendanceDto row = mock(OrgEmployeeAttendanceDto.class);
        PageImpl<OrgEmployeeAttendanceDto> page =
                new PageImpl<>(List.of(row), PageRequest.of(0, 1), 1);
        when(dashboardRepository.findOrgEmployeeAttendance(eq(ORG_ID), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<OrgEmployeeAttendanceDto> result =
                service.getOrgEmployeeAttendance(ORG_ID, FROM, TO, 0, 20);

        assertThat(result.getContent()).containsExactly(row);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.isFirst()).isTrue();
        assertThat(result.isLast()).isTrue();
    }

    @Test
    void getOrgEmployeeAttendance_emptyPage_returnsEmptyResponse() {
        PageImpl<OrgEmployeeAttendanceDto> emptyPage =
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(dashboardRepository.findOrgEmployeeAttendance(eq(ORG_ID), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(emptyPage);

        PaginatedResponse<OrgEmployeeAttendanceDto> result =
                service.getOrgEmployeeAttendance(ORG_ID, FROM, TO, 0, 20);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ── Org: location comparison ──────────────────────────────────────────────

    @Test
    void getOrgLocationComparison_returnsRepositoryResult() {
        OrgLocationComparisonDto row = mock(OrgLocationComparisonDto.class);
        when(dashboardRepository.findOrgLocationComparison(ORG_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getOrgLocationComparison(ORG_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findOrgLocationComparison(ORG_ID, FROM, TO);
    }

    @Test
    void getOrgLocationComparison_nullDates_delegatesNullsToRepository() {
        when(dashboardRepository.findOrgLocationComparison(ORG_ID, null, null)).thenReturn(List.of());

        service.getOrgLocationComparison(ORG_ID, null, null);

        verify(dashboardRepository).findOrgLocationComparison(ORG_ID, null, null);
    }

    // ── Analytics: daily summary ──────────────────────────────────────────────

    @Test
    void getAttendanceDailySummary_returnsRepositoryResult() {
        AttendanceSummaryDto summary = new AttendanceSummaryDto(DATE, 30, 5, 2, 10, 3, 0, 1);
        when(dashboardRepository.findAttendanceDailySummary(LOCATION_ID, DATE)).thenReturn(summary);

        assertThat(service.getAttendanceDailySummary(LOCATION_ID, DATE)).isSameAs(summary);
        verify(dashboardRepository).findAttendanceDailySummary(LOCATION_ID, DATE);
    }

    // ── Analytics: attendance detail (paginated) ──────────────────────────────

    @Test
    void getAttendanceDetail_wrapsPageInPaginatedResponse() {
        AttendanceDetailDto row = mock(AttendanceDetailDto.class);
        PageImpl<AttendanceDetailDto> page =
                new PageImpl<>(List.of(row), PageRequest.of(0, 1), 5);
        when(dashboardRepository.findAttendanceDetail(eq(LOCATION_ID), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(page);

        PaginatedResponse<AttendanceDetailDto> result =
                service.getAttendanceDetail(LOCATION_ID, FROM, TO, 0, 20);

        assertThat(result.getContent()).containsExactly(row);
        assertThat(result.getTotalElements()).isEqualTo(5);
    }

    @Test
    void getAttendanceDetail_emptyPage_returnsEmptyContent() {
        PageImpl<AttendanceDetailDto> emptyPage =
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(dashboardRepository.findAttendanceDetail(eq(LOCATION_ID), eq(FROM), eq(TO), any(Pageable.class)))
                .thenReturn(emptyPage);

        assertThat(service.getAttendanceDetail(LOCATION_ID, FROM, TO, 0, 20).getContent()).isEmpty();
    }

    // ── Analytics: CSV export ─────────────────────────────────────────────────

    @Test
    void streamAttendanceExportCsv_writesHeaderAndDataRow() {
        AttendanceExportDto row = new AttendanceExportDto(
                UUID.randomUUID(), "Jane Doe", "EMP-001", "Engineering", "Backend", "Senior",
                UUID.randomUUID(), DATE, "PRESENT",
                LocalDateTime.of(2026, 4, 15, 8, 0),
                LocalDateTime.of(2026, 4, 15, 17, 0),
                480, 0, false, false);
        when(dashboardRepository.streamAttendanceExport(LOCATION_ID, FROM, TO))
                .thenReturn(Stream.of(row));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamAttendanceExportCsv(LOCATION_ID, FROM, TO, out);

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).startsWith("Employee Name,Employee Code");
        assertThat(csv).contains("Jane Doe");
        assertThat(csv).contains("Engineering");
        assertThat(csv).contains("PRESENT");
        assertThat(csv).contains("480");
    }

    @Test
    void streamAttendanceExportCsv_noRows_writesOnlyHeader() {
        when(dashboardRepository.streamAttendanceExport(LOCATION_ID, FROM, TO))
                .thenReturn(Stream.empty());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamAttendanceExportCsv(LOCATION_ID, FROM, TO, out);

        assertThat(out.toString(StandardCharsets.UTF_8).trim()).isEqualTo(
                "Employee Name,Employee Code,Department,Team,Rank,Date,Status,First Badge In (Local),Last Badge Out (Local),Duration (mins),Minutes Late,Late,Overridden");
    }

    @Test
    void streamAttendanceExportCsv_nullTimestamps_writesEmptyFields() {
        AttendanceExportDto row = new AttendanceExportDto(
                UUID.randomUUID(), "Alice", "EMP-002", "Finance", null, null,
                UUID.randomUUID(), DATE, "ABSENT",
                null, null, null, null, false, false);
        when(dashboardRepository.streamAttendanceExport(LOCATION_ID, FROM, TO))
                .thenReturn(Stream.of(row));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamAttendanceExportCsv(LOCATION_ID, FROM, TO, out);

        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv).contains("Alice");
        assertThat(csv).contains("ABSENT");
    }

    // ── Analytics: chronic absenteeism ────────────────────────────────────────

    @Test
    void getChronicAbsenteeism_returnsRepositoryResult() {
        ChronicAbsenteeismDto row = mock(ChronicAbsenteeismDto.class);
        when(dashboardRepository.findChronicAbsenteeism(LOCATION_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getChronicAbsenteeism(LOCATION_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findChronicAbsenteeism(LOCATION_ID, FROM, TO);
    }

    @Test
    void getChronicAbsenteeism_nullDates_delegatesNullsToRepository() {
        when(dashboardRepository.findChronicAbsenteeism(LOCATION_ID, null, null)).thenReturn(List.of());

        service.getChronicAbsenteeism(LOCATION_ID, null, null);

        verify(dashboardRepository).findChronicAbsenteeism(LOCATION_ID, null, null);
    }

    // ── Analytics: remote usage ───────────────────────────────────────────────

    @Test
    void getRemoteUsageSummary_returnsRepositoryResult() {
        RemoteUsageSummaryDto row = mock(RemoteUsageSummaryDto.class);
        when(dashboardRepository.findRemoteUsageSummary(LOCATION_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getRemoteUsageSummary(LOCATION_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findRemoteUsageSummary(LOCATION_ID, FROM, TO);
    }

    // ── Analytics: override audit ─────────────────────────────────────────────

    @Test
    void getOverrideAudit_returnsRepositoryResult() {
        AttendanceOverrideAuditDto row = mock(AttendanceOverrideAuditDto.class);
        when(dashboardRepository.findOverrideAudit(LOCATION_ID, FROM, TO)).thenReturn(List.of(row));

        assertThat(service.getOverrideAudit(LOCATION_ID, FROM, TO)).containsExactly(row);
        verify(dashboardRepository).findOverrideAudit(LOCATION_ID, FROM, TO);
    }

    @Test
    void getOverrideAudit_emptyResult_returnsEmptyList() {
        when(dashboardRepository.findOverrideAudit(LOCATION_ID, FROM, TO)).thenReturn(List.of());

        assertThat(service.getOverrideAudit(LOCATION_ID, FROM, TO)).isEmpty();
    }
}
