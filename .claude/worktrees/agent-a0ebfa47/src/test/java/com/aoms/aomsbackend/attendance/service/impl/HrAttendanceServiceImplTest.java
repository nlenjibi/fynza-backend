package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.AttendanceDetailDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceExportDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryDto;
import com.aoms.aomsbackend.attendance.dto.AttendanceSummaryResponse;
import com.aoms.aomsbackend.attendance.dto.HrAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.repository.EmployeeRepository;
import com.aoms.aomsbackend.attendance.repository.HrAttendanceRepository;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrAttendanceServiceImplTest {

    private static final String CSV_HEADER =
            "Employee Name,Employee Code,Department,Team,Rank,Date,Status,First Badge In (Local),Last Badge Out (Local),Duration (mins),Minutes Late,Late,Overridden";

    @Mock private EmployeeRepository employeeRepository;
    @Mock private HrAttendanceRepository hrAttendanceRepository;
    @InjectMocks private HrAttendanceServiceImpl service;

    @Test
    void getLocationAttendance_blankDepartmentNormalizesToNull() {
        UUID locationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        AttendanceDetailDto dto = detailDto(employeeId, "Alice Smith", "Engineering", false);
        Page<AttendanceDetailDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 1), 1);

        when(employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, null))
                .thenReturn(List.of(employeeId));
        when(hrAttendanceRepository.findByLocationAndUsers(eq(locationId), any(), any(), any(), any(), any()))
                .thenReturn(page);

        PaginatedResponse<HrAttendanceRecordResponse> response = service.getLocationAttendance(
                locationId, "   ", null, null, null, null, null, 0, 20, Sort.Direction.DESC);

        verify(employeeRepository).findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, null);
        assertThat(response.getContent()).hasSize(1);
        HrAttendanceRecordResponse first = response.getContent().getFirst();
        assertThat(first.getEmployeeId()).isEqualTo(employeeId);
        assertThat(first.getEmployeeName()).isEqualTo("Alice Smith");
        assertThat(first.getDepartment()).isEqualTo("Engineering");
        assertThat(first.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(first.isOverridden()).isFalse();
    }

    @Test
    void getLocationAttendance_returnsEmptyPageWhenNoEmployeesFound() {
        UUID locationId = UUID.randomUUID();

        when(employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, null))
                .thenReturn(List.of());

        PaginatedResponse<HrAttendanceRecordResponse> response = service.getLocationAttendance(
                locationId, null, null, null, null, null, null, 0, 20, Sort.Direction.ASC);

        verify(hrAttendanceRepository, never()).findByLocationAndUsers(any(), any(), any(), any(), any(), any());
        assertThat(response.getContent()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }

    @Test
    void getLocationAttendance_singleEmployeeFilterSkipsEmployeeQuery() {
        UUID locationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();

        AttendanceDetailDto dto = detailDto(employeeId, "Bob Jones", "Finance", true);
        Page<AttendanceDetailDto> page = new PageImpl<>(List.of(dto), PageRequest.of(0, 1), 1);

        when(hrAttendanceRepository.findByLocationAndUsers(eq(locationId), eq(List.of(employeeId)), any(), any(), any(), any()))
                .thenReturn(page);

        PaginatedResponse<HrAttendanceRecordResponse> response = service.getLocationAttendance(
                locationId, null, null, employeeId, null, null, null, 0, 20, Sort.Direction.ASC);

        verify(employeeRepository, never()).findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(any(), any());
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().getFirst().isOverridden()).isTrue();
    }

    @Test
    void getLocationAttendance_statusFilterConvertsToStrings() {
        UUID locationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        List<AttendanceStatus> statuses = List.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE);
        List<String> expectedStatusStrings = List.of("PRESENT", "LATE");

        when(employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, "Engineering"))
                .thenReturn(List.of(employeeId));
        when(hrAttendanceRepository.findByLocationAndUsers(
                eq(locationId), any(), any(), any(), eq(expectedStatusStrings), any()))
                .thenReturn(Page.empty());

        service.getLocationAttendance(
                locationId, "Engineering", null, null, null, null, statuses, 0, 20, Sort.Direction.ASC);

        verify(hrAttendanceRepository).findByLocationAndUsers(
                eq(locationId), any(), any(), any(), eq(expectedStatusStrings), any());
    }

    @Test
    void getLocationAttendance_nullStatusPassesAllStatusStringsToRepository() {
        UUID locationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        int totalStatuses = AttendanceStatus.values().length;

        when(employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, null))
                .thenReturn(List.of(employeeId));
        when(hrAttendanceRepository.findByLocationAndUsers(any(), any(), any(), any(), any(), any()))
                .thenReturn(Page.empty());

        service.getLocationAttendance(
                locationId, null, null, null, null, null, null, 0, 20, Sort.Direction.ASC);

        verify(hrAttendanceRepository).findByLocationAndUsers(
                any(), any(), any(), any(),
                argThatHasSize(totalStatuses),
                any());
    }

    @Test
    void streamHrAttendanceCsv_writesHeaderAndDataRow() {
        UUID locationId = UUID.randomUUID();
        UUID employeeId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 28);

        AttendanceExportDto row = exportDto(employeeId, date);

        when(employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, null))
                .thenReturn(List.of(employeeId));
        when(hrAttendanceRepository.streamByLocationAndUsers(eq(locationId), any(), any(), any(), any()))
                .thenReturn(Stream.of(row));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamHrAttendanceCsv(locationId, null, null, null, date, date, null, out);

        String csv = out.toString(StandardCharsets.UTF_8);
        String[] lines = csv.split("\\r?\\n");
        assertThat(lines[0]).isEqualTo(CSV_HEADER);
        assertThat(lines[1])
                .contains("Jane Doe")
                .contains("EMP-001")
                .contains("HR")
                .contains("2026-04-28")
                .contains("PRESENT");
    }

    @Test
    void streamHrAttendanceCsv_emptyStreamWhenNoEmployeesFound() {
        UUID locationId = UUID.randomUUID();

        when(employeeRepository.findActiveEmployeeIdsByPrimaryBuildingIdAndDepartment(locationId, null))
                .thenReturn(List.of());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamHrAttendanceCsv(locationId, null, null, null,
                LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 28), null, out);

        verify(hrAttendanceRepository, never()).streamByLocationAndUsers(any(), any(), any(), any(), any());
        String csv = out.toString(StandardCharsets.UTF_8);
        assertThat(csv.trim()).isEqualTo(CSV_HEADER);
    }

    @Test
    void getSummary_returnsEmptyTotalsWhenDtoMissing() {
        UUID locationId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 28);
        when(hrAttendanceRepository.getSummary(locationId, date)).thenReturn(null);

        AttendanceSummaryResponse response = service.getSummary(locationId, date);

        assertThat(response.getDate()).isEqualTo(date);
        assertThat(response.getTotalPresent()).isZero();
        assertThat(response.getTotalLate()).isZero();
        assertThat(response.getTotalAbsent()).isZero();
    }

    @Test
    void getSummary_mapsDtoCounts() {
        UUID locationId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 4, 28);
        AttendanceSummaryDto dto = new AttendanceSummaryDto(date, 30, 5, 2, 10, 3, 0, 1);
        when(hrAttendanceRepository.getSummary(locationId, date)).thenReturn(dto);

        AttendanceSummaryResponse response = service.getSummary(locationId, date);

        assertThat(response.getTotalPresent()).isEqualTo(30L);
        assertThat(response.getTotalLate()).isEqualTo(5L);
        assertThat(response.getTotalAbsent()).isEqualTo(2L);
        assertThat(response.getTotalRemote()).isEqualTo(10L);
        assertThat(response.getTotalOnLeave()).isEqualTo(3L);
        assertThat(response.getTotalInsufficientHours()).isEqualTo(1L);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private AttendanceDetailDto detailDto(UUID employeeId, String name, String department, boolean overridden) {
        return new AttendanceDetailDto(
                employeeId, name, "EMP-001", department, "Engineer", "Alpha",
                UUID.randomUUID(), LocalDate.of(2026, 4, 28), "PRESENT",
                OffsetDateTime.of(2026, 4, 28, 8, 0, 0, 0, ZoneOffset.UTC),
                OffsetDateTime.of(2026, 4, 28, 17, 0, 0, 0, ZoneOffset.UTC),
                540, false, 0, overridden, null);
    }

    private AttendanceExportDto exportDto(UUID employeeId, LocalDate date) {
        return new AttendanceExportDto(
                employeeId, "Jane Doe", "EMP-001", "HR", "Talent", "Senior",
                UUID.randomUUID(), date, "PRESENT",
                LocalDateTime.of(2026, 4, 28, 8, 0),
                LocalDateTime.of(2026, 4, 28, 17, 0),
                540, 0, false, false);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> argThatHasSize(int size) {
        return org.mockito.ArgumentMatchers.argThat(list -> list != null && ((List<T>) list).size() == size);
    }
}
