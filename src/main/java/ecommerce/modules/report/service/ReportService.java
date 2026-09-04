package ecommerce.modules.report.service;

import ecommerce.common.enums.ReportType;
import ecommerce.modules.report.dto.*;
import ecommerce.modules.report.entity.Report;
import ecommerce.modules.report.entity.ReportSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ReportService {

    ReportResponse createReport(ReportRequest request, UUID userId);

    ReportResponse getReportById(UUID reportId);

    Page<ReportResponse> getReports(Pageable pageable);

    Page<ReportResponse> searchReports(
            ReportType reportType,
            Report.ReportStatus status,
            UUID createdBy,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable);

    byte[] downloadReport(UUID reportId);

    ReportResponse regenerateReport(UUID reportId);

    Map<String, Object> getAvailableReportTypes();

    ReportScheduleResponse createSchedule(ReportScheduleRequest request, UUID userId);

    ReportScheduleResponse getScheduleById(UUID scheduleId);

    Page<ReportScheduleResponse> getSchedules(Pageable pageable);

    Page<ReportScheduleResponse> getSchedulesByStatus(ReportSchedule.ScheduleStatus status, Pageable pageable);

    ReportScheduleResponse updateSchedule(UUID scheduleId, ReportScheduleRequest request);

    ReportScheduleResponse pauseSchedule(UUID scheduleId);

    ReportScheduleResponse resumeSchedule(UUID scheduleId);

    ReportScheduleResponse deleteSchedule(UUID scheduleId);
}
