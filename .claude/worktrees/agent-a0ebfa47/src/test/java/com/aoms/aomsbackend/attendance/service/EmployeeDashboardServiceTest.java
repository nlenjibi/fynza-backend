package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.TodayStatusResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceFlag;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceMonthlySummary;
import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceSelfView;
import com.aoms.aomsbackend.attendance.entity.EmployeeHoursSummary;
import com.aoms.aomsbackend.attendance.entity.EmployeePunctualitySummary;
import com.aoms.aomsbackend.attendance.entity.EmployeeTodayStatus;
import com.aoms.aomsbackend.attendance.repository.EmployeeAttendanceMonthlySummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeAttendanceSelfViewRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeHoursSummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeePunctualitySummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeTodayStatusRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeWeeklyRemoteUsageHistoryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeWeeklyRemoteUsageRepository;
import com.aoms.aomsbackend.attendance.service.impl.AttendanceServiceImpl;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeDashboardServiceTest {

    @Mock private EmployeeAttendanceSelfViewRepository attendanceView;
    @Mock private EmployeeTodayStatusRepository todayStatusRepo;
    @Mock private EmployeeWeeklyRemoteUsageRepository weeklyRemoteRepo;
    @Mock private EmployeeAttendanceMonthlySummaryRepository monthlySummaryRepo;
    @Mock private EmployeePunctualitySummaryRepository punctualityRepo;
    @Mock private EmployeeHoursSummaryRepository hoursSummaryRepo;
    @Mock private EmployeeWeeklyRemoteUsageHistoryRepository remoteHistoryRepo;
    @Mock private HttpServletRequest httpRequest;
    @Mock private HttpSession session;

    private AttendanceServiceImpl service;

    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new AttendanceServiceImpl(
                attendanceView, todayStatusRepo, weeklyRemoteRepo,
                monthlySummaryRepo, punctualityRepo, hoursSummaryRepo, remoteHistoryRepo);
        when(httpRequest.getSession(false)).thenReturn(session);
        when(session.getAttribute(SessionAttribute.USER_ID.getKey())).thenReturn(USER_ID.toString());
    }

    // ── getMyTodayStatus ───────────────────────────────────────────────────────

    @Test
    void getMyTodayStatus_whenRecordExists_returnsMappedResponse() {
        // Arrange
        EmployeeTodayStatus entity = EmployeeTodayStatus.builder()
                .userId(USER_ID)
                .status("PRESENT")
                .firstBadgeIn(LocalDateTime.of(2026, 4, 29, 8, 5))
                .isLate(false)
                .minutesLate(0)
                .hoursReached(true)
                .build();
        when(todayStatusRepo.findByUserId(USER_ID)).thenReturn(Optional.of(entity));

        // Act
        TodayStatusResponse result = service.getMyTodayStatus(httpRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(AttendanceStatus.PRESENT);
        assertThat(result.getFirstBadgeIn()).isEqualTo(LocalDateTime.of(2026, 4, 29, 8, 5));
    }

    @Test
    void getMyTodayStatus_whenNoRecord_returnsNull() {
        // Arrange
        when(todayStatusRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        TodayStatusResponse result = service.getMyTodayStatus(httpRequest);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    void getMyTodayStatus_withExpiredSession_throwsSessionExpiredException() {
        // Arrange
        when(httpRequest.getSession(false)).thenReturn(null);

        // Act + Assert
        assertThatThrownBy(() -> service.getMyTodayStatus(httpRequest))
                .isInstanceOf(SessionExpiredException.class);
    }

    // ── getMyWeeklyRemoteUsage ─────────────────────────────────────────────────

    @Test
    void getMyWeeklyRemoteUsage_whenRecordExists_computesFillPct() {
        // Arrange
        EmployeeWeeklyRemoteUsage entity = EmployeeWeeklyRemoteUsage.builder()
                .userId(USER_ID)
                .remoteDaysUsed(3)
                .weeklyLimit(5)
                .daysRemaining(2)
                .weekStartDate(LocalDate.of(2026, 4, 27))
                .weekEndDate(LocalDate.of(2026, 5, 1))
                .build();
        when(weeklyRemoteRepo.findByUserId(USER_ID)).thenReturn(Optional.of(entity));

        // Act
        WeeklyRemoteUsageResponse result = service.getMyWeeklyRemoteUsage(httpRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getFillPct()).isEqualTo(60.0);
    }

    @Test
    void getMyWeeklyRemoteUsage_whenNoRecord_returnsNull() {
        // Arrange
        when(weeklyRemoteRepo.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // Act
        WeeklyRemoteUsageResponse result = service.getMyWeeklyRemoteUsage(httpRequest);

        // Assert
        assertThat(result).isNull();
    }

    // ── getMonthlySummary ──────────────────────────────────────────────────────

    @Test
    void getMonthlySummary_withYearAndMonth_returnsSingleResult() {
        // Arrange
        EmployeeAttendanceMonthlySummary entity = EmployeeAttendanceMonthlySummary.builder()
                .userId(USER_ID)
                .year(2026)
                .month(4)
                .presentDays(18)
                .lateDays(2)
                .attendanceRatePct(90.0)
                .build();
        when(monthlySummaryRepo.findByUserIdAndYearAndMonth(USER_ID, 2026, 4))
                .thenReturn(Optional.of(entity));

        // Act
        List<MonthlySummaryResponse> result = service.getMonthlySummary(httpRequest, 2026, 4);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMonth()).isEqualTo(4);
        assertThat(result.get(0).getPresentDays()).isEqualTo(18);
    }

    @Test
    void getMonthlySummary_withYearOnly_returnsYearList() {
        // Arrange
        List<EmployeeAttendanceMonthlySummary> entities = List.of(
                EmployeeAttendanceMonthlySummary.builder().userId(USER_ID).year(2026).month(1).build(),
                EmployeeAttendanceMonthlySummary.builder().userId(USER_ID).year(2026).month(2).build(),
                EmployeeAttendanceMonthlySummary.builder().userId(USER_ID).year(2026).month(3).build()
        );
        when(monthlySummaryRepo.findByUserIdAndYearOrderByMonthAsc(USER_ID, 2026))
                .thenReturn(entities);

        // Act
        List<MonthlySummaryResponse> result = service.getMonthlySummary(httpRequest, 2026, null);

        // Assert
        assertThat(result).hasSize(3);
    }

    // ── getPunctualitySummary ──────────────────────────────────────────────────

    @Test
    void getPunctualitySummary_withYearAndMonth_returnsMapped() {
        // Arrange
        EmployeePunctualitySummary entity = EmployeePunctualitySummary.builder()
                .userId(USER_ID)
                .year(2026)
                .month(4)
                .lateDays(3)
                .onTimeRatePct(85.0)
                .build();
        when(punctualityRepo.findByUserIdAndYearAndMonth(USER_ID, 2026, 4))
                .thenReturn(Optional.of(entity));

        // Act
        List<PunctualitySummaryResponse> result = service.getPunctualitySummary(httpRequest, 2026, 4);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLateDays()).isEqualTo(3);
        assertThat(result.get(0).getOnTimeRatePct()).isEqualTo(85.0);
    }

    // ── getHoursSummary ────────────────────────────────────────────────────────

    @Test
    void getHoursSummary_withYearAndMonth_returnsMapped() {
        // Arrange
        EmployeeHoursSummary entity = EmployeeHoursSummary.builder()
                .userId(USER_ID)
                .year(2026)
                .month(4)
                .totalHoursWorked(160.0)
                .avgDailyHours(8.0)
                .build();
        when(hoursSummaryRepo.findByUserIdAndYearAndMonth(USER_ID, 2026, 4))
                .thenReturn(Optional.of(entity));

        // Act
        List<HoursSummaryResponse> result = service.getHoursSummary(httpRequest, 2026, 4);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTotalHoursWorked()).isEqualTo(160.0);
    }

    // ── getRemoteUsageHistory ──────────────────────────────────────────────────

    @Test
    void getRemoteUsageHistory_limitsResults() {
        // Arrange — produce 15 entities, request limit=12
        List<EmployeeWeeklyRemoteUsageHistory> entities = new ArrayList<>();
        for (int w = 15; w >= 1; w--) {
            entities.add(EmployeeWeeklyRemoteUsageHistory.builder()
                    .userId(USER_ID)
                    .year(2026)
                    .weekNumber(w)
                    .remoteDaysUsed(1)
                    .weeklyLimit(2)
                    .overLimit(false)
                    .build());
        }
        when(remoteHistoryRepo.findByUserIdAndYearOrderByWeekNumberDesc(USER_ID, 2026))
                .thenReturn(entities);

        // Act
        List<WeeklyRemoteUsageHistoryResponse> result =
                service.getRemoteUsageHistory(httpRequest, 2026, 12);

        // Assert
        assertThat(result).hasSize(12);
    }

    // ── getMyAttendance flag routing ───────────────────────────────────────────

    @Test
    void getMyAttendance_withLateFlag_callsIsLateRepository() {
        // Arrange
        when(attendanceView.findByUserIdAndRecordDateBetweenAndIsLate(
                eq(USER_ID), any(), any(), eq(true), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        service.getMyAttendance(httpRequest, null, null, null, AttendanceFlag.LATE, 0, 20, "desc");

        // Assert
        verify(attendanceView).findByUserIdAndRecordDateBetweenAndIsLate(
                eq(USER_ID), any(), any(), eq(true), any());
    }

    @Test
    void getMyAttendance_withOnTimeFlag_callsIsLateRepositoryFalse() {
        // Arrange
        when(attendanceView.findByUserIdAndRecordDateBetweenAndIsLate(
                eq(USER_ID), any(), any(), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        service.getMyAttendance(httpRequest, null, null, null, AttendanceFlag.ON_TIME, 0, 20, "desc");

        // Assert
        verify(attendanceView).findByUserIdAndRecordDateBetweenAndIsLate(
                eq(USER_ID), any(), any(), eq(false), any());
    }
}
