package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordDetailResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceSelfView;
import com.aoms.aomsbackend.attendance.repository.EmployeeAttendanceMonthlySummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeAttendanceSelfViewRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeHoursSummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeePunctualitySummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeTodayStatusRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeWeeklyRemoteUsageHistoryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeWeeklyRemoteUsageRepository;
import com.aoms.aomsbackend.attendance.service.impl.AttendanceServiceImpl;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.common.exception.AttendanceRecordNotFoundException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceTest {

    @Mock private EmployeeAttendanceSelfViewRepository attendanceView;
    @Mock private EmployeeTodayStatusRepository todayStatusRepo;
    @Mock private EmployeeWeeklyRemoteUsageRepository weeklyRemoteRepo;
    @Mock private EmployeeAttendanceMonthlySummaryRepository monthlySummaryRepo;
    @Mock private EmployeePunctualitySummaryRepository punctualityRepo;
    @Mock private EmployeeHoursSummaryRepository hoursSummaryRepo;
    @Mock private EmployeeWeeklyRemoteUsageHistoryRepository remoteHistoryRepo;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpSession session;

    private AttendanceServiceImpl attendanceService;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceServiceImpl(
                attendanceView, todayStatusRepo, weeklyRemoteRepo,
                monthlySummaryRepo, punctualityRepo, hoursSummaryRepo, remoteHistoryRepo);
        when(httpRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(USER_ID.toString());
    }

    // ── getMyAttendance: fromDate handling ─────────────────────────────────────

    @Test
    void getMyAttendance_withNullFromDate_uses1900Floor() {
        // Arrange — view already guards employment start; service uses 1900-01-01 as floor
        when(attendanceView.findByUserIdAndRecordDateBetween(
                eq(USER_ID), eq(LocalDate.of(1900, 1, 1)), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        attendanceService.getMyAttendance(httpRequest, null, null, null, null, 0, 20, "desc");

        // Assert
        verify(attendanceView).findByUserIdAndRecordDateBetween(
                eq(USER_ID), eq(LocalDate.of(1900, 1, 1)), any(), any());
    }

    @Test
    void getMyAttendance_withFromDate_passesItThrough() {
        // Arrange — view's employment-start guard is transparent; service passes date unchanged
        LocalDate requestedFrom = LocalDate.of(2026, 1, 1);
        when(attendanceView.findByUserIdAndRecordDateBetween(
                eq(USER_ID), eq(requestedFrom), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        attendanceService.getMyAttendance(httpRequest, requestedFrom, null, null, null, 0, 20, "desc");

        // Assert
        verify(attendanceView).findByUserIdAndRecordDateBetween(
                eq(USER_ID), eq(requestedFrom), any(), any());
    }

    // ── getMyAttendance: status filter routing ─────────────────────────────────

    @Test
    void getMyAttendance_withStatusFilter_usesFilteredQuery() {
        // Arrange
        List<AttendanceStatus> statuses = List.of(AttendanceStatus.PRESENT, AttendanceStatus.LATE);
        List<String> statusStrings = List.of("PRESENT", "LATE");
        when(attendanceView.findByUserIdAndRecordDateBetweenAndStatusIn(
                eq(USER_ID), any(), any(), eq(statusStrings), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act

        attendanceService.getMyAttendance(httpRequest, null, null, statuses, null, 0, 20, "desc");

        // Assert
        verify(attendanceView).findByUserIdAndRecordDateBetweenAndStatusIn(
                eq(USER_ID), any(), any(), eq(statusStrings), any());
    }

    @Test
    void getMyAttendance_withEmptyStatusList_usesUnfilteredQuery() {
        // Arrange
        when(attendanceView.findByUserIdAndRecordDateBetween(
                eq(USER_ID), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        attendanceService.getMyAttendance(httpRequest, null, null, List.of(), null, 0, 20, "desc");

        // Assert — empty list treated same as null
        verify(attendanceView).findByUserIdAndRecordDateBetween(eq(USER_ID), any(), any(), any());
    }

    // ── getMyAttendance: inverted date range ───────────────────────────────────

    @Test
    void getMyAttendance_withToDateBeforeFromDate_returnsEmptyWithoutHittingDb() {
        // Arrange — fromDate in the future relative to toDate
        LocalDate from = LocalDate.of(2026, 6, 1);
        LocalDate to   = LocalDate.of(2026, 1, 1);

        // Act
        PaginatedResponse<AttendanceRecordResponse> result =
                attendanceService.getMyAttendance(httpRequest, from, to, null, null, 0, 20, "desc");

        // Assert — inverted range short-circuits before hitting the DB
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
        verifyNoInteractions(attendanceView);
    }

    // ── getMyAttendance: pagination ────────────────────────────────────────────

    @Test
    void getMyAttendance_withPagination_passesCorrectPageable() {
        // Arrange — any() in stub so it fires regardless of pageable value
        when(attendanceView.findByUserIdAndRecordDateBetween(
                eq(USER_ID), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 5), 0));

        // Act
        attendanceService.getMyAttendance(httpRequest, null, null, null, null, 2, 5, "asc");

        // Assert — page=2, size=5, ASC sort must be forwarded
        verify(attendanceView).findByUserIdAndRecordDateBetween(
                eq(USER_ID), any(), any(),
                eq(PageRequest.of(2, 5,
                        org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.ASC, "recordDate"))));
    }

    // ── getMyAttendance: badge field mapping ───────────────────────────────────

    @Test
    void getMyAttendance_withBadgeData_populatesBadgeFields() {
        // Arrange
        EmployeeAttendanceSelfView row = buildViewRow(null, "PRESENT", UUID.randomUUID(), 15);
        when(attendanceView.findByUserIdAndRecordDateBetween(eq(USER_ID), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));

        // Act
        PaginatedResponse<AttendanceRecordResponse> result =
                attendanceService.getMyAttendance(httpRequest, null, null, null, null, 0, 20, "desc");

        // Assert
        AttendanceRecordResponse dto = result.getContent().get(0);
        assertThat(dto.getFirstBadgeIn()).isEqualTo(row.getFirstBadgeIn());
        assertThat(dto.getTotalDurationMinutes()).isEqualTo(568);
        assertThat(dto.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(dto.getIsLate()).isTrue();
        assertThat(dto.getMinutesLate()).isEqualTo(15);
    }

    @Test
    void getMyAttendance_withNullBadges_returnsNullBadgeFields() {
        // Arrange — REMOTE day has no work session → all badge fields null
        EmployeeAttendanceSelfView row = buildViewRow(null, "REMOTE", null, null);
        when(attendanceView.findByUserIdAndRecordDateBetween(eq(USER_ID), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(row)));

        // Act
        PaginatedResponse<AttendanceRecordResponse> result =
                attendanceService.getMyAttendance(httpRequest, null, null, null, null, 0, 20, "desc");

        // Assert — no NPE, badge fields null
        AttendanceRecordResponse dto = result.getContent().get(0);
        assertThat(dto.getFirstBadgeIn()).isNull();
        assertThat(dto.getLastBadgeOut()).isNull();
        assertThat(dto.getStatus()).isEqualTo(AttendanceStatus.REMOTE);
    }

    // ── getMyAttendanceRecord ──────────────────────────────────────────────────

    @Test
    void getMyAttendanceRecord_withValidId_returnsDetailWithBadgeData() {
        // Arrange
        UUID recordId = UUID.randomUUID();
        EmployeeAttendanceSelfView row = buildViewRow(recordId, "PRESENT", UUID.randomUUID(), 0);

        when(attendanceView.findByIdAndUserIdNative(recordId, USER_ID)).thenReturn(Optional.of(row));

        // Act
        AttendanceRecordDetailResponse result =
                attendanceService.getMyAttendanceRecord(httpRequest, recordId);

        // Assert
        assertThat(result.getId()).isEqualTo(recordId);
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getFirstBadgeIn()).isEqualTo(row.getFirstBadgeIn());
        assertThat(result.getWorkSessionId()).isEqualTo(row.getWorkSessionId());
        assertThat(result.getIsLate()).isFalse();  // minutesLate = 0
    }

    @Test
    void getMyAttendanceRecord_withNullBadges_returnsNullBadgeFields() {
        // Arrange — ON_LEAVE record has no work session
        UUID recordId = UUID.randomUUID();
        EmployeeAttendanceSelfView row = buildViewRow(recordId, "ON_LEAVE", null, null);

        when(attendanceView.findByIdAndUserIdNative(recordId, USER_ID)).thenReturn(Optional.of(row));

        // Act
        AttendanceRecordDetailResponse result =
                attendanceService.getMyAttendanceRecord(httpRequest, recordId);

        // Assert — no NPE, badge fields null
        assertThat(result.getFirstBadgeIn()).isNull();
        assertThat(result.getWorkSessionId()).isNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ON_LEAVE);
    }

    @Test
    void getMyAttendanceRecord_withUnknownId_throwsNotFoundException() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        when(attendanceView.findByIdAndUserIdNative(unknownId, USER_ID)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> attendanceService.getMyAttendanceRecord(httpRequest, unknownId))
                .isInstanceOf(AttendanceRecordNotFoundException.class);
    }

    @Test
    void getMyAttendanceRecord_withOtherUsersId_throwsNotFoundException() {
        // Arrange — findByIdAndUserId returns empty because userId filter excludes it
        UUID otherId = UUID.randomUUID();
        when(attendanceView.findByIdAndUserIdNative(otherId, USER_ID)).thenReturn(Optional.empty());

        // Act + Assert — must be 404, not 403, to avoid information disclosure
        assertThatThrownBy(() -> attendanceService.getMyAttendanceRecord(httpRequest, otherId))
                .isInstanceOf(AttendanceRecordNotFoundException.class);
    }

    // ── session resolution ─────────────────────────────────────────────────────

    @Test
    void getMyAttendance_withNoSession_throwsSessionExpiredException() {
        when(httpRequest.getSession(false)).thenReturn(null);

        assertThatThrownBy(() ->
                attendanceService.getMyAttendance(httpRequest, null, null, null, null, 0, 20, "desc"))
                .isInstanceOf(SessionExpiredException.class);
    }

    @Test
    void getMyAttendance_withV2AuthSession_resolvesUserByV2UserId() {
        // Arrange — V1 key absent; V2 key present
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(null);
        when(session.getAttribute(SessionAttribute.V2_USER_ID.getKey())).thenReturn(USER_ID.toString());
        when(attendanceView.findByUserIdAndRecordDateBetween(eq(USER_ID), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        PaginatedResponse<AttendanceRecordResponse> result =
                attendanceService.getMyAttendance(httpRequest, null, null, null, null, 0, 20, "desc");

        // Assert
        assertThat(result).isNotNull();
        verify(attendanceView).findByUserIdAndRecordDateBetween(eq(USER_ID), any(), any(), any());
    }

    @Test
    void getMyAttendance_withNoUserIdInSession_throwsSessionExpiredException() {
        // Arrange — both V1 and V2 keys missing
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(null);
        when(session.getAttribute(SessionAttribute.V2_USER_ID.getKey())).thenReturn(null);

        assertThatThrownBy(() ->
                attendanceService.getMyAttendance(httpRequest, null, null, null, null, 0, 20, "desc"))
                .isInstanceOf(SessionExpiredException.class);
    }

    // ── helpers ────────────────────────────────────────────────────────────────

    private EmployeeAttendanceSelfView buildViewRow(UUID id, String status, UUID workSessionId, Integer minutesLate) {
        boolean hasBadges = workSessionId != null;
        boolean late = minutesLate != null && minutesLate > 0;
        return EmployeeAttendanceSelfView.builder()
                .id(id != null ? id : UUID.randomUUID())
                .userId(USER_ID)
                .recordDate(LocalDate.of(2026, 4, 10))
                .status(status)
                .workSessionId(workSessionId)
                .isOverridden(false)
                .firstBadgeIn(hasBadges ? LocalDateTime.of(2026, 4, 10, 8, 2) : null)
                .lastBadgeOut(hasBadges ? LocalDateTime.of(2026, 4, 10, 17, 30) : null)
                .totalDurationMinutes(hasBadges ? 568 : null)
                .minutesLate(minutesLate)
                .isLate(late)
                .build();
    }
}
