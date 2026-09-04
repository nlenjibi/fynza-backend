package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.CalendarDayResponse;
import com.aoms.aomsbackend.attendance.dto.CalendarRecordEntry;
import com.aoms.aomsbackend.attendance.dto.TeamAttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.*;
import com.aoms.aomsbackend.attendance.exception.EmployeeNotFoundException;
import com.aoms.aomsbackend.attendance.exception.ExportWindowTooLargeException;
import com.aoms.aomsbackend.attendance.exception.NotADirectReportException;
import com.aoms.aomsbackend.attendance.repository.AttendanceRecordRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeRepository;
import com.aoms.aomsbackend.attendance.service.impl.TeamAttendanceServiceImpl;
import com.aoms.aomsbackend.auth.entity.User;
import com.aoms.aomsbackend.auth.repository.UserRepository;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamAttendanceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private AttendanceRecordRepository attendanceRecordRepository;

    @InjectMocks
    private TeamAttendanceServiceImpl service;

    private UUID authUserId;
    private UUID managerEmployeeId;
    private UUID buildingId;
    private User authUser;
    private Employee managerEmployee;
    private Employee directReport1;
    private Employee directReport2;
    private Employee directReport3;

    @BeforeEach
    void setUp() {
        authUserId = UUID.randomUUID();
        managerEmployeeId = UUID.randomUUID();
        buildingId = UUID.randomUUID();

        authUser = User.builder()
                .id(authUserId)
                .ssoUserId("sso-manager-123")
                .firstName("Manager")
                .lastName("One")
                .email("manager@test.com")
                .build();

        managerEmployee = Employee.builder()
                .id(managerEmployeeId)
                .ssoUserId("sso-manager-123")
                .firstName("Manager")
                .lastName("One")
                .primaryBuildingId(buildingId)
                .active(true)
                .employmentStartDate(LocalDate.of(2020, 1, 1))
                .build();

        directReport1 = Employee.builder()
                .id(UUID.randomUUID())
                .ssoUserId("sso-dr1")
                .firstName("Alice")
                .lastName("Smith")
                .managerId(managerEmployeeId)
                .primaryBuildingId(buildingId)
                .active(true)
                .employmentStartDate(LocalDate.of(2021, 1, 1))
                .build();

        directReport2 = Employee.builder()
                .id(UUID.randomUUID())
                .ssoUserId("sso-dr2")
                .firstName("Bob")
                .lastName("Jones")
                .managerId(managerEmployeeId)
                .primaryBuildingId(buildingId)
                .active(true)
                .employmentStartDate(LocalDate.of(2021, 6, 1))
                .build();

        directReport3 = Employee.builder()
                .id(UUID.randomUUID())
                .ssoUserId("sso-dr3")
                .firstName("Carol")
                .lastName("White")
                .managerId(managerEmployeeId)
                .primaryBuildingId(buildingId)
                .active(true)
                .employmentStartDate(LocalDate.of(2022, 1, 1))
                .build();
    }

    private void stubManagerResolution() {
        when(userRepository.findById(authUserId)).thenReturn(Optional.of(authUser));
        when(employeeRepository.findBySsoUserId("sso-manager-123")).thenReturn(Optional.of(managerEmployee));
        when(employeeRepository.findByManagerIdAndPrimaryBuildingIdAndActiveTrueAndDeletedAtIsNull(
                managerEmployeeId, buildingId))
                .thenReturn(List.of(directReport1, directReport2, directReport3));
    }

    @Test
    void getTeamAttendance_returnsRecordsForDirectReports() {
        stubManagerResolution();

        AttendanceRecord record = buildRecord(directReport1.getId(), LocalDate.of(2026, 3, 14), AttendanceStatus.PRESENT);

        when(attendanceRecordRepository.findTeamAttendance(anyList(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "recordDate"));
        PaginatedResponse<TeamAttendanceRecordResponse> result = service.getTeamAttendance(
                authUserId, null, null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEmployeeId()).isEqualTo(directReport1.getId());
        assertThat(result.getContent().getFirst().getEmployeeName()).isEqualTo("Alice Smith");
        assertThat(result.getContent().getFirst().getStatus()).isEqualTo(AttendanceStatus.PRESENT);
    }

    @Test
    void getTeamAttendance_withEmployeeIdFilter_returnsFilteredRecords() {
        stubManagerResolution();

        AttendanceRecord record = buildRecord(directReport1.getId(), LocalDate.of(2026, 3, 14), AttendanceStatus.LATE);

        when(attendanceRecordRepository.findTeamAttendance(eq(List.of(directReport1.getId())), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(record)));

        Pageable pageable = PageRequest.of(0, 20);
        PaginatedResponse<TeamAttendanceRecordResponse> result = service.getTeamAttendance(
                authUserId, directReport1.getId(), null, null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getEmployeeId()).isEqualTo(directReport1.getId());
    }

    @Test
    void getTeamAttendance_filterByNonDirectReport_throwsForbidden() {
        stubManagerResolution();

        UUID nonReportId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getTeamAttendance(authUserId, nonReportId, null, null, null, pageable))
                .isInstanceOf(NotADirectReportException.class);
    }

    @Test
    void getTeamAttendance_userNotFound_throwsNotFound() {
        when(userRepository.findById(authUserId)).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getTeamAttendance(authUserId, null, null, null, null, pageable))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void getTeamAttendance_employeeRecordNotFound_throwsNotFound() {
        when(userRepository.findById(authUserId)).thenReturn(Optional.of(authUser));
        when(employeeRepository.findBySsoUserId("sso-manager-123")).thenReturn(Optional.empty());

        Pageable pageable = PageRequest.of(0, 20);

        assertThatThrownBy(() -> service.getTeamAttendance(authUserId, null, null, null, null, pageable))
                .isInstanceOf(EmployeeNotFoundException.class);
    }

    @Test
    void getTeamCalendar_returnsAllDirectReportsWithNullForMissing() {
        stubManagerResolution();

        LocalDate date = LocalDate.of(2026, 3, 14);
        AttendanceRecord record1 = buildRecord(directReport1.getId(), date, AttendanceStatus.PRESENT);

        when(attendanceRecordRepository.findByUserIdsAndDate(anyList(), eq(date)))
                .thenReturn(List.of(record1));

        CalendarDayResponse result = service.getTeamCalendar(authUserId, date);

        assertThat(result.getDate()).isEqualTo(date);
        assertThat(result.getRecords()).hasSize(3);

        CalendarRecordEntry entry1 = result.getRecords().stream()
                .filter(e -> e.getEmployeeId().equals(directReport1.getId()))
                .findFirst().orElseThrow();
        assertThat(entry1.getStatus()).isEqualTo(AttendanceStatus.PRESENT);

        CalendarRecordEntry entry2 = result.getRecords().stream()
                .filter(e -> e.getEmployeeId().equals(directReport2.getId()))
                .findFirst().orElseThrow();
        assertThat(entry2.getStatus()).isNull();
        assertThat(entry2.getIsOverridden()).isNull();
    }

    @Test
    void streamCsv_windowExceeds90Days_throwsExportWindowTooLarge() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 4, 2); // 91 days

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> service.streamTeamAttendanceCsv(authUserId, from, to, null, null, out))
                .isInstanceOf(ExportWindowTooLargeException.class);
    }

    @Test
    void streamCsv_validWindow_writesHeaderAndRows() {
        stubManagerResolution();

        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);

        WorkSession ws = WorkSession.builder()
                .id(UUID.randomUUID())
                .firstBadgeIn(Instant.parse("2026-03-14T08:00:00Z"))
                .lastBadgeOut(Instant.parse("2026-03-14T17:00:00Z"))
                .totalDurationMinutes(540)
                .isLate(false)
                .minutesLate(0)
                .build();

        AttendanceRecord record = AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .userId(directReport1.getId())
                .buildingId(buildingId)
                .officeId(UUID.randomUUID())
                .recordDate(LocalDate.of(2026, 3, 14))
                .status(AttendanceStatus.PRESENT)
                .workSessionId(ws.getId())
                .workSession(ws)
                .overridden(false)
                .passRunId(UUID.randomUUID())
                .build();

        when(attendanceRecordRepository.streamTeamAttendanceList(anyList(), eq(from), eq(to), isNull()))
                .thenReturn(Stream.of(record));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        service.streamTeamAttendanceCsv(authUserId, from, to, null, null, out);

        String csv = out.toString();
        String[] lines = csv.split("\n");
        assertThat(lines).hasSize(2);
        assertThat(lines[0].trim()).startsWith("Employee Name,Employee ID");
        assertThat(lines[1]).contains("Alice Smith");
        assertThat(lines[1]).contains("PRESENT");
        assertThat(lines[1]).contains("540");
    }

    @Test
    void streamCsv_nonDirectReportFilter_throwsForbidden() {
        stubManagerResolution();

        UUID nonReportId = UUID.randomUUID();
        LocalDate from = LocalDate.of(2026, 3, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        assertThatThrownBy(() -> service.streamTeamAttendanceCsv(authUserId, from, to, nonReportId, null, out))
                .isInstanceOf(NotADirectReportException.class);
    }

    private AttendanceRecord buildRecord(UUID userId, LocalDate date, AttendanceStatus status) {
        return AttendanceRecord.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .buildingId(buildingId)
                .officeId(UUID.randomUUID())
                .recordDate(date)
                .status(status)
                .overridden(false)
                .passRunId(UUID.randomUUID())
                .build();
    }
}
