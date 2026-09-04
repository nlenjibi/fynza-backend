package com.aoms.aomsbackend.attendance.service;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordDetailResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.dto.TodayStatusResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceFlag;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceService {

    PaginatedResponse<AttendanceRecordResponse> getMyAttendance(
            HttpServletRequest request,
            LocalDate fromDate,
            LocalDate toDate,
            List<AttendanceStatus> statuses,
            AttendanceFlag flag,
            int page,
            int size,
            String order
    );

    AttendanceRecordDetailResponse getMyAttendanceRecord(
            HttpServletRequest request,
            UUID recordId
    );

    TodayStatusResponse getMyTodayStatus(HttpServletRequest request);

    WeeklyRemoteUsageResponse getMyWeeklyRemoteUsage(HttpServletRequest request);

    List<MonthlySummaryResponse> getMonthlySummary(
            HttpServletRequest request, int year, Integer month);

    List<PunctualitySummaryResponse> getPunctualitySummary(
            HttpServletRequest request, int year, Integer month);

    List<HoursSummaryResponse> getHoursSummary(
            HttpServletRequest request, int year, Integer month);

    List<WeeklyRemoteUsageHistoryResponse> getRemoteUsageHistory(
            HttpServletRequest request, int year, int limit);
}
