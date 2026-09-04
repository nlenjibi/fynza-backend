package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.AttendanceRecordDetailResponse;
import com.aoms.aomsbackend.attendance.dto.AttendanceRecordResponse;
import com.aoms.aomsbackend.attendance.dto.TodayStatusResponse;
import com.aoms.aomsbackend.attendance.entity.AttendanceFlag;
import com.aoms.aomsbackend.attendance.entity.AttendanceStatus;
import com.aoms.aomsbackend.attendance.service.AttendanceService;
import com.aoms.aomsbackend.auth.entity.UserRoleType;
import com.aoms.aomsbackend.common.annotation.RequiresRole;
import com.aoms.aomsbackend.common.responses.PaginatedResponse;
import com.aoms.aomsbackend.common.responses.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
@RequiredArgsConstructor
@RequiresRole(UserRoleType.EMPLOYEE)
@Tag(name = "Attendance", description = "Employee attendance self-view endpoints")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @Operation(
            summary = "Get my attendance history",
            description = "Returns a paginated list of attendance records for the authenticated employee.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Attendance records returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my")
    public ResponseEntity<ResponseWrapper<PaginatedResponse<AttendanceRecordResponse>>> getMyAttendance(
            HttpServletRequest request,
            @Parameter(description = "Start date (ISO-8601).")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date (ISO-8601). Defaults to today.")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Filter by one or more statuses (repeatable).")
            @RequestParam(required = false) List<AttendanceStatus> statuses,
            @Parameter(description = "Filter by late/on-time flag.")
            @RequestParam(required = false) AttendanceFlag flag,
            @Parameter(description = "Zero-based page index. Defaults to 0.")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1–100). Defaults to 20.")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort direction: asc or desc. Defaults to desc.")
            @RequestParam(defaultValue = "desc") String order) {

        PaginatedResponse<AttendanceRecordResponse> result =
                attendanceService.getMyAttendance(request, fromDate, toDate, statuses, flag, page, size, order);

        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get a single attendance record",
            description = "Returns the full detail for one attendance record.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Record returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired"),
                    @ApiResponse(responseCode = "404", description = "Record not found")
            }
    )
    @GetMapping("/my/{recordId}")
    public ResponseEntity<ResponseWrapper<AttendanceRecordDetailResponse>> getMyAttendanceRecord(
            HttpServletRequest request,
            @Parameter(description = "UUID of the attendance record.")
            @PathVariable UUID recordId) {

        AttendanceRecordDetailResponse result =
                attendanceService.getMyAttendanceRecord(request, recordId);

        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get today's attendance status",
            description = "Returns today's live attendance status for the authenticated employee.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Today's status returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my/today")
    public ResponseEntity<ResponseWrapper<TodayStatusResponse>> getMyTodayStatus(
            HttpServletRequest request) {

        TodayStatusResponse result = attendanceService.getMyTodayStatus(request);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get current week remote usage",
            description = "Returns the current week's remote day usage and limit for the authenticated employee.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Weekly remote usage returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my/remote/weekly")
    public ResponseEntity<ResponseWrapper<WeeklyRemoteUsageResponse>> getMyWeeklyRemoteUsage(
            HttpServletRequest request) {

        WeeklyRemoteUsageResponse result = attendanceService.getMyWeeklyRemoteUsage(request);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get monthly attendance summary",
            description = "Returns attendance summary for a given year, optionally filtered to a specific month.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Monthly summary returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my/summary/monthly")
    public ResponseEntity<ResponseWrapper<List<MonthlySummaryResponse>>> getMonthlySummary(
            HttpServletRequest request,
            @Parameter(description = "Year to query.")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @Parameter(description = "Optional month (1-12). If absent, returns all months for the year.")
            @RequestParam(required = false) Integer month) {

        List<MonthlySummaryResponse> result = attendanceService.getMonthlySummary(request, year, month);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get punctuality summary",
            description = "Returns punctuality summary for a given year, optionally filtered to a specific month.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Punctuality summary returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my/summary/punctuality")
    public ResponseEntity<ResponseWrapper<List<PunctualitySummaryResponse>>> getPunctualitySummary(
            HttpServletRequest request,
            @Parameter(description = "Year to query.")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @Parameter(description = "Optional month (1-12). If absent, returns all months for the year.")
            @RequestParam(required = false) Integer month) {

        List<PunctualitySummaryResponse> result = attendanceService.getPunctualitySummary(request, year, month);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get hours worked summary",
            description = "Returns hours worked summary for a given year, optionally filtered to a specific month.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Hours summary returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my/summary/hours")
    public ResponseEntity<ResponseWrapper<List<HoursSummaryResponse>>> getHoursSummary(
            HttpServletRequest request,
            @Parameter(description = "Year to query.")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @Parameter(description = "Optional month (1-12). If absent, returns all months for the year.")
            @RequestParam(required = false) Integer month) {

        List<HoursSummaryResponse> result = attendanceService.getHoursSummary(request, year, month);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Get remote usage history",
            description = "Returns weekly remote usage history for a given year, limited by the limit parameter.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Remote usage history returned"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/my/remote/history")
    public ResponseEntity<ResponseWrapper<List<WeeklyRemoteUsageHistoryResponse>>> getRemoteUsageHistory(
            HttpServletRequest request,
            @Parameter(description = "Year to query.")
            @RequestParam(defaultValue = "#{T(java.time.LocalDate).now().getYear()}") int year,
            @Parameter(description = "Maximum number of weeks to return. Defaults to 12.")
            @RequestParam(defaultValue = "12") int limit) {

        List<WeeklyRemoteUsageHistoryResponse> result =
                attendanceService.getRemoteUsageHistory(request, year, limit);
        return ResponseEntity.ok(ResponseWrapper.success(result));
    }
}
