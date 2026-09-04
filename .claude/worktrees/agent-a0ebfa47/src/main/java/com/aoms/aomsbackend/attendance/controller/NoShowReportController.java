package com.aoms.aomsbackend.attendance.controller;

import com.aoms.aomsbackend.attendance.dto.NoShowReportRecordDto;
import com.aoms.aomsbackend.attendance.service.NoShowReportService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance/no-shows")
@RequiredArgsConstructor
@RequiresRole(UserRoleType.FACILITIES_ADMIN)
@Tag(name = "No-Show Report", description = "Facilities admin no-show reporting endpoints")
public class NoShowReportController {

    private final NoShowReportService noShowReportService;

    @Operation(
            summary = "Get no-show report",
            description = "Returns a paginated no-show report. "
                    + "Accessible by FACILITIES_ADMIN and SUPER_ADMIN only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Report returned"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping
    public ResponseEntity<ResponseWrapper<PaginatedResponse<NoShowReportRecordDto>>> getReport(
            HttpServletRequest request,
            @Parameter(description = "Start date (ISO-8601, inclusive).")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date (ISO-8601, inclusive).")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Filter by employee UUID (optional).")
            @RequestParam(required = false) UUID employeeId,
            @Parameter(description = "Filter by department name (optional).")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter by organisation UUID (SUPER_ADMIN only, optional).")
            @RequestParam(required = false) UUID organisationId,
            @Parameter(description = "Zero-based page index. Defaults to 0.")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (1–100). Defaults to 20.")
            @RequestParam(defaultValue = "20") int size) {

        PaginatedResponse<NoShowReportRecordDto> result = noShowReportService.getReport(
                request, fromDate, toDate, employeeId, department, organisationId, page, size);

        return ResponseEntity.ok(ResponseWrapper.success(result));
    }

    @Operation(
            summary = "Export no-show report as CSV",
            description = "Streams a CSV file. Max 90-day window. "
                    + "Accessible by FACILITIES_ADMIN and SUPER_ADMIN only.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "CSV stream returned"),
                    @ApiResponse(responseCode = "400", description = "Export window exceeds 90 days"),
                    @ApiResponse(responseCode = "403", description = "Insufficient role"),
                    @ApiResponse(responseCode = "401", description = "Session invalid or expired")
            }
    )
    @GetMapping("/export")
    public ResponseEntity<StreamingResponseBody> exportCsv(
            HttpServletRequest request,
            @Parameter(description = "Start date (ISO-8601, inclusive).")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date (ISO-8601, inclusive).")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "Filter by employee UUID (optional).")
            @RequestParam(required = false) UUID employeeId,
            @Parameter(description = "Filter by department name (optional).")
            @RequestParam(required = false) String department,
            @Parameter(description = "Filter by organisation UUID (SUPER_ADMIN only, optional).")
            @RequestParam(required = false) UUID organisationId) {

        StreamingResponseBody body = noShowReportService.exportCsv(
                request, fromDate, toDate, employeeId, department, organisationId);

        String filename = "no-show-report-" + fromDate + "-" + toDate + ".csv";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(body);
    }
}
