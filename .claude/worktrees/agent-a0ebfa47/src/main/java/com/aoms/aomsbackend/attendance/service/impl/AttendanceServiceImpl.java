package com.aoms.aomsbackend.attendance.service.impl;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordDetailResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.dto.TodayStatusResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceFlag;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.entity.EmployeeAttendanceSelfView;
import com.aoms.aomsbackend.attendance.repository.EmployeeAttendanceMonthlySummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeAttendanceSelfViewRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeHoursSummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeePunctualitySummaryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeTodayStatusRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeWeeklyRemoteUsageHistoryRepository;
import com.aoms.aomsbackend.attendance.repository.EmployeeWeeklyRemoteUsageRepository;
import com.aoms.aomsbackend.attendance.service.AttendanceService;
import com.aoms.aomsbackend.auth.constant.SessionAttribute;
import com.aoms.aomsbackend.common.exception.AttendanceRecordNotFoundException;
import com.aoms.aomsbackend.common.exception.SessionExpiredException;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceServiceImpl implements AttendanceService {

    private final EmployeeAttendanceSelfViewRepository attendanceView;
    private final EmployeeTodayStatusRepository todayStatusRepo;
    private final EmployeeWeeklyRemoteUsageRepository weeklyRemoteRepo;
    private final EmployeeAttendanceMonthlySummaryRepository monthlySummaryRepo;
    private final EmployeePunctualitySummaryRepository punctualityRepo;
    private final EmployeeHoursSummaryRepository hoursSummaryRepo;
    private final EmployeeWeeklyRemoteUsageHistoryRepository remoteHistoryRepo;

    @Override
    public PaginatedResponse<AttendanceRecordResponse> getMyAttendance(
            HttpServletRequest request,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            AttendanceFlag flag,
            int page,
            int size,
            String order) {

        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        LocalDate effectiveFrom = fromDate != null ? fromDate : LocalDate.of(1900, 1, 1);
        LocalDate effectiveTo = toDate != null ? toDate : LocalDate.now();

        if (effectiveTo.isBefore(effectiveFrom)) {
            return PaginatedResponse.from(Page.empty(PageRequest.of(page, size)));
        }

        Pageable pageable = buildPageable(page, size, order);
        Page<EmployeeAttendanceSelfView> recordPage = queryAttendancePage(
                userId, effectiveFrom, effectiveTo, statuses, flag, pageable);

        return PaginatedResponse.from(recordPage.map(this::toListDto));
    }

    private Page<EmployeeAttendanceSelfView> queryAttendancePage(
            UUID userId, LocalDate from, LocalDate to,
            List<AttendanceStatus> statuses, AttendanceFlag flag, Pageable pageable) {

        boolean hasStatus = statuses != null && !statuses.isEmpty();
        List<String> statusStrings = hasStatus ? toStatusStrings(statuses) : null;

        if (flag == AttendanceFlag.LATE) {
            return hasStatus
                    ? attendanceView.findByUserIdAndRecordDateBetweenAndIsLateAndStatusIn(
                            userId, from, to, true, statusStrings, pageable)
                    : attendanceView.findByUserIdAndRecordDateBetweenAndIsLate(
                            userId, from, to, true, pageable);
        }
        if (flag == AttendanceFlag.ON_TIME) {
            return hasStatus
                    ? attendanceView.findByUserIdAndRecordDateBetweenAndIsLateAndStatusIn(
                            userId, from, to, false, statusStrings, pageable)
                    : attendanceView.findByUserIdAndRecordDateBetweenAndIsLate(
                            userId, from, to, false, pageable);
        }
        return hasStatus
                ? attendanceView.findByUserIdAndRecordDateBetweenAndStatusIn(
                        userId, from, to, statusStrings, pageable)
                : attendanceView.findByUserIdAndRecordDateBetween(userId, from, to, pageable);
    }

    @Override
    public AttendanceRecordDetailResponse getMyAttendanceRecord(
            HttpServletRequest request, UUID recordId) {

        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        EmployeeAttendanceSelfView row = attendanceView
                .findByIdAndUserIdNative(recordId, userId)
                .orElseThrow(AttendanceRecordNotFoundException::new);

        return AttendanceRecordDetailResponse.from(row);
    }

    @Override
    public TodayStatusResponse getMyTodayStatus(HttpServletRequest request) {
        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        return todayStatusRepo.findByUserId(userId)
                .map(TodayStatusResponse::from)
                .orElse(null);
    }

    @Override
    public WeeklyRemoteUsageResponse getMyWeeklyRemoteUsage(HttpServletRequest request) {
        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        return weeklyRemoteRepo.findByUserId(userId)
                .map(WeeklyRemoteUsageResponse::from)
                .orElse(null);
    }

    @Override
    public List<MonthlySummaryResponse> getMonthlySummary(
            HttpServletRequest request, int year, Integer month) {

        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        if (month != null) {
            return monthlySummaryRepo.findByUserIdAndYearAndMonth(userId, year, month)
                    .map(row -> List.of(MonthlySummaryResponse.from(row)))
                    .orElse(List.of());
        }
        return monthlySummaryRepo.findByUserIdAndYearOrderByMonthAsc(userId, year)
                .stream()
                .map(MonthlySummaryResponse::from)
                .toList();
    }

    @Override
    public List<PunctualitySummaryResponse> getPunctualitySummary(
            HttpServletRequest request, int year, Integer month) {

        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        if (month != null) {
            return punctualityRepo.findByUserIdAndYearAndMonth(userId, year, month)
                    .map(row -> List.of(PunctualitySummaryResponse.from(row)))
                    .orElse(List.of());
        }
        return punctualityRepo.findByUserIdAndYearOrderByMonthAsc(userId, year)
                .stream()
                .map(PunctualitySummaryResponse::from)
                .toList();
    }

    @Override
    public List<HoursSummaryResponse> getHoursSummary(
            HttpServletRequest request, int year, Integer month) {

        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        if (month != null) {
            return hoursSummaryRepo.findByUserIdAndYearAndMonth(userId, year, month)
                    .map(row -> List.of(HoursSummaryResponse.from(row)))
                    .orElse(List.of());
        }
        return hoursSummaryRepo.findByUserIdAndYearOrderByMonthAsc(userId, year)
                .stream()
                .map(HoursSummaryResponse::from)
                .toList();
    }

    @Override
    public List<WeeklyRemoteUsageHistoryResponse> getRemoteUsageHistory(
            HttpServletRequest request, int year, int limit) {

        HttpSession session = requireSession(request);
        UUID userId = resolveUserId(session);

        return remoteHistoryRepo.findByUserIdAndYearOrderByWeekNumberDesc(userId, year)
                .stream()
                .limit(limit)
                .map(WeeklyRemoteUsageHistoryResponse::from)
                .toList();
    }

    // ── session helpers ────────────────────────────────────────────────────────

    private HttpSession requireSession(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new SessionExpiredException("Session is invalid or expired.");
        }
        return session;
    }

    private UUID resolveUserId(HttpSession session) {
        String userId = (String) session.getAttribute(SessionAttribute.USER_ID.getKey());
        if (userId == null) {
            userId = (String) session.getAttribute(SessionAttribute.V2_USER_ID.getKey());
        }
        if (userId == null) {
            throw new SessionExpiredException("Session is invalid or expired.");
        }
        return UUID.fromString(userId);
    }

    // ── query helpers ──────────────────────────────────────────────────────────

    private List<String> toStatusStrings(List<AttendanceStatus> statuses) {
        return statuses.stream()
                .map(AttendanceStatus::name)
                .toList();
    }

    // ── mapping helpers ────────────────────────────────────────────────────────

    private AttendanceRecordResponse toListDto(EmployeeAttendanceSelfView row) {
        return AttendanceRecordResponse.builder()
                .id(row.getId())
                .recordDate(row.getRecordDate())
                .status(parseStatus(row.getStatus()))
                .firstBadgeIn(row.getFirstBadgeIn())
                .lastBadgeOut(row.getLastBadgeOut())
                .totalDurationMinutes(row.getTotalDurationMinutes())
                .isLate(row.getIsLate())
                .minutesLate(row.getMinutesLate())
                .isOverridden(row.getIsOverridden())
                .hoursReached(row.getHoursReached())
                .build();
    }

    private AttendanceStatus parseStatus(String status) {
        if (status == null) return null;
        try {
            return AttendanceStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return AttendanceStatus.ABSENT;
        }
    }

    private Pageable buildPageable(int page, int size, String order) {
        int safePage = Math.max(0, page);
        int safeSize = Math.clamp(size, 1, 100);
        Sort.Direction direction = "asc".equalsIgnoreCase(order)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        return PageRequest.of(safePage, safeSize, Sort.by(direction, "recordDate"));
    }
}
