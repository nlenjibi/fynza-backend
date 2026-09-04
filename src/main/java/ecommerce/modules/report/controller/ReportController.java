package ecommerce.modules.report.controller;

import ecommerce.common.enums.ReportType;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.report.dto.*;
import ecommerce.modules.report.entity.Report;
import ecommerce.modules.report.entity.ReportSchedule;
import ecommerce.modules.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/reports")
@RequiredArgsConstructor
@Tag(name = "Admin Reports", description = "Admin report generation and scheduling endpoints")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/types")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get available report types", description = "Get list of available report types and formats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAvailableReportTypes() {
        Map<String, Object> types = reportService.getAvailableReportTypes();
        return ResponseEntity.ok(ApiResponse.success("Report types retrieved successfully", types));
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate report", description = "Generate a new report with specified parameters")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @Valid @RequestBody ReportRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ReportResponse report = reportService.createReport(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Report generation started", report));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all reports", description = "Get all generated reports with optional filters")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReportResponse>>> getAllReports(
            @RequestParam(required = false) ReportType reportType,
            @RequestParam(required = false) Report.ReportStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ReportResponse> reports = reportService.searchReports(reportType, status, null, dateFrom, dateTo, pageable);
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully",
                PaginatedResponse.from(reports)));
    }

    @GetMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get report by ID", description = "Get details of a specific report")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(@PathVariable UUID reportId) {
        ReportResponse report = reportService.getReportById(reportId);
        return ResponseEntity.ok(ApiResponse.success("Report retrieved successfully", report));
    }

    @GetMapping("/{reportId}/download")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Download report", description = "Download a generated report")
    public ResponseEntity<byte[]> downloadReport(@PathVariable UUID reportId) {
        ReportResponse report = reportService.getReportById(reportId);
        
        if (!"COMPLETED".equals(report.getStatus())) {
            return ResponseEntity.badRequest().body(("Report is not ready for download. Status: " + report.getStatus()).getBytes());
        }
        
        byte[] data = reportService.downloadReport(reportId);
        
        String filename = report.getReportNumber() + "." + report.getFormat().name().toLowerCase();
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(report.getFormat().getMimeType()))
                .body(data);
    }

    @PostMapping("/{reportId}/regenerate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Regenerate report", description = "Regenerate an existing report")
    public ResponseEntity<ApiResponse<ReportResponse>> regenerateReport(@PathVariable UUID reportId) {
        ReportResponse report = reportService.regenerateReport(reportId);
        return ResponseEntity.ok(ApiResponse.success("Report regeneration started", report));
    }

    @GetMapping("/scheduled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get scheduled reports", description = "Get all scheduled reports")
    public ResponseEntity<ApiResponse<PaginatedResponse<ReportScheduleResponse>>> getScheduledReports(
            @RequestParam(required = false) ReportSchedule.ScheduleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<ReportScheduleResponse> schedules = status != null 
                ? reportService.getSchedulesByStatus(status, pageable)
                : reportService.getSchedules(pageable);
        return ResponseEntity.ok(ApiResponse.success("Scheduled reports retrieved successfully",
                PaginatedResponse.from(schedules)));
    }

    @PostMapping("/scheduled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create scheduled report", description = "Create a new scheduled report")
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> createScheduledReport(
            @Valid @RequestBody ReportScheduleRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID userId = UUID.fromString(userDetails.getUsername());
        ReportScheduleResponse schedule = reportService.createSchedule(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report created successfully", schedule));
    }

    @GetMapping("/scheduled/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get scheduled report by ID", description = "Get details of a specific scheduled report")
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> getScheduledReportById(@PathVariable UUID scheduleId) {
        ReportScheduleResponse schedule = reportService.getScheduleById(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report retrieved successfully", schedule));
    }

    @PutMapping("/scheduled/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update scheduled report", description = "Update an existing scheduled report")
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> updateScheduledReport(
            @PathVariable UUID scheduleId,
            @Valid @RequestBody ReportScheduleRequest request) {
        ReportScheduleResponse schedule = reportService.updateSchedule(scheduleId, request);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report updated successfully", schedule));
    }

    @PatchMapping("/scheduled/{scheduleId}/pause")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Pause scheduled report", description = "Pause a scheduled report")
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> pauseScheduledReport(@PathVariable UUID scheduleId) {
        ReportScheduleResponse schedule = reportService.pauseSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report paused successfully", schedule));
    }

    @PatchMapping("/scheduled/{scheduleId}/resume")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Resume scheduled report", description = "Resume a paused scheduled report")
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> resumeScheduledReport(@PathVariable UUID scheduleId) {
        ReportScheduleResponse schedule = reportService.resumeSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report resumed successfully", schedule));
    }

    @DeleteMapping("/scheduled/{scheduleId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete scheduled report", description = "Delete (stop) a scheduled report")
    public ResponseEntity<ApiResponse<ReportScheduleResponse>> deleteScheduledReport(@PathVariable UUID scheduleId) {
        ReportScheduleResponse schedule = reportService.deleteSchedule(scheduleId);
        return ResponseEntity.ok(ApiResponse.success("Scheduled report deleted successfully", schedule));
    }
}
