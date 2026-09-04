package ecommerce.modules.report.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.report.dto.*;
import ecommerce.modules.report.entity.Report;
import ecommerce.modules.report.entity.ReportSchedule;
import ecommerce.modules.report.repository.ReportRepository;
import ecommerce.modules.report.repository.ReportScheduleRepository;
import ecommerce.modules.report.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ReportScheduleRepository reportScheduleRepository;
    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ReportResponse createReport(ReportRequest request, UUID userId) {
        String reportNumber = Report.generateReportNumber();
        
        String title = request.getTitle() != null ? request.getTitle() 
                : request.getReportType().getDisplayName() + " Report";
        
        Report report = Report.builder()
                .reportNumber(reportNumber)
                .reportType(request.getReportType())
                .title(title)
                .description(request.getDescription())
                .format(request.getFormat())
                .status(Report.ReportStatus.PENDING)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(userId)
                .filters(serializeFilters(request.getFilters()))
                .build();

        Report saved = reportRepository.save(report);
        
        generateReportAsync(saved.getId());
        
        return mapToResponse(saved);
    }

    @Async("reportExecutor")
    public void generateReportAsync(UUID reportId) {
        try {
            Report report = reportRepository.findById(reportId)
                    .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
            
            report.setStatus(Report.ReportStatus.PROCESSING);
            reportRepository.save(report);
            
            byte[] reportData = generateReportData(report);
            
            String filePath = "/reports/" + report.getReportNumber() + "." + report.getFormat().name().toLowerCase();
            
            report.setStatus(Report.ReportStatus.COMPLETED);
            report.setFilePath(filePath);
            report.setFileSize((long) reportData.length);
            report.setCompletedAt(LocalDateTime.now());
            
            reportRepository.save(report);
            log.info("Report generated successfully: {}", report.getReportNumber());
            
        } catch (Exception e) {
            log.error("Failed to generate report: {}", e.getMessage());
            Report report = reportRepository.findById(reportId).orElse(null);
            if (report != null) {
                report.setStatus(Report.ReportStatus.FAILED);
                report.setErrorMessage(e.getMessage());
                reportRepository.save(report);
            }
        }
    }

    @Override
    public ReportResponse getReportById(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        return mapToResponse(report);
    }

    @Override
    public Page<ReportResponse> getReports(Pageable pageable) {
        return reportRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<ReportResponse> searchReports(
            ReportType reportType,
            Report.ReportStatus status,
            UUID createdBy,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            Pageable pageable) {
        return reportRepository.searchReports(reportType, status, createdBy, dateFrom, dateTo, pageable)
                .map(this::mapToResponse);
    }

    @Override
    public byte[] downloadReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        
        if (report.getStatus() != Report.ReportStatus.COMPLETED) {
            throw new IllegalStateException("Report is not ready for download");
        }
        
        return generateReportData(report);
    }

    @Override
    @Transactional
    public ReportResponse regenerateReport(UUID reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report not found"));
        
        report.setStatus(Report.ReportStatus.PENDING);
        report.setFilePath(null);
        report.setFileSize(null);
        report.setCompletedAt(null);
        report.setErrorMessage(null);
        
        reportRepository.save(report);
        generateReportAsync(reportId);
        
        return mapToResponse(report);
    }

    @Override
    public Map<String, Object> getAvailableReportTypes() {
        Map<String, Object> reportTypes = new HashMap<>();
        for (ReportType type : ReportType.values()) {
            Map<String, String> typeInfo = new HashMap<>();
            typeInfo.put("name", type.name());
            typeInfo.put("displayName", type.getDisplayName());
            typeInfo.put("description", type.getDescription());
            typeInfo.put("color", type.getColor());
            reportTypes.put(type.name(), typeInfo);
        }
        
        Map<String, String> formats = new HashMap<>();
        for (ReportFormat format : ReportFormat.values()) {
            formats.put(format.name(), format.getDisplayName());
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("reportTypes", reportTypes);
        response.put("formats", formats);
        
        return response;
    }

    @Override
    @Transactional
    public ReportScheduleResponse createSchedule(ReportScheduleRequest request, UUID userId) {
        if (reportScheduleRepository.findByScheduleNameAndCreatedBy(request.getScheduleName(), userId).isPresent()) {
            throw new IllegalArgumentException("Schedule with this name already exists");
        }
        
        String cronExpression = buildCronExpression(request);
        LocalDateTime nextRun = calculateNextRun(request);
        
        ReportSchedule schedule = ReportSchedule.builder()
                .scheduleName(request.getScheduleName())
                .reportType(request.getReportType())
                .scheduleType(ReportSchedule.ScheduleType.valueOf(request.getScheduleType().name()))
                .format(request.getFormat())
                .status(ReportSchedule.ScheduleStatus.ACTIVE)
                .cronExpression(cronExpression)
                .dayOfWeek(request.getDayOfWeek())
                .dayOfMonth(request.getDayOfMonth())
                .hour(request.getHour())
                .minute(request.getMinute())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .nextRunAt(nextRun)
                .createdBy(userId)
                .filters(serializeFilters(request.getFilters()))
                .recipients(String.join(",", request.getEmailRecipients() != null ? request.getEmailRecipients() : Collections.emptyList()))
                .build();

        ReportSchedule saved = reportScheduleRepository.save(schedule);
        return mapScheduleToResponse(saved);
    }

    @Override
    public ReportScheduleResponse getScheduleById(UUID scheduleId) {
        ReportSchedule schedule = reportScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        return mapScheduleToResponse(schedule);
    }

    @Override
    public Page<ReportScheduleResponse> getSchedules(Pageable pageable) {
        return reportScheduleRepository.findAll(pageable).map(this::mapScheduleToResponse);
    }

    @Override
    public Page<ReportScheduleResponse> getSchedulesByStatus(ReportSchedule.ScheduleStatus status, Pageable pageable) {
        return reportScheduleRepository.findByStatus(status, pageable).map(this::mapScheduleToResponse);
    }

    @Override
    @Transactional
    public ReportScheduleResponse updateSchedule(UUID scheduleId, ReportScheduleRequest request) {
        ReportSchedule schedule = reportScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        schedule.setScheduleName(request.getScheduleName());
        schedule.setReportType(request.getReportType());
        schedule.setScheduleType(ReportSchedule.ScheduleType.valueOf(request.getScheduleType().name()));
        schedule.setFormat(request.getFormat());
        schedule.setDayOfWeek(request.getDayOfWeek());
        schedule.setDayOfMonth(request.getDayOfMonth());
        schedule.setHour(request.getHour());
        schedule.setMinute(request.getMinute());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setFilters(serializeFilters(request.getFilters()));
        schedule.setRecipients(String.join(",", request.getEmailRecipients() != null ? request.getEmailRecipients() : Collections.emptyList()));
        schedule.setCronExpression(buildCronExpression(request));
        schedule.setNextRunAt(calculateNextRun(request));
        
        ReportSchedule saved = reportScheduleRepository.save(schedule);
        return mapScheduleToResponse(saved);
    }

    @Override
    @Transactional
    public ReportScheduleResponse pauseSchedule(UUID scheduleId) {
        ReportSchedule schedule = reportScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        schedule.setStatus(ReportSchedule.ScheduleStatus.PAUSED);
        ReportSchedule saved = reportScheduleRepository.save(schedule);
        return mapScheduleToResponse(saved);
    }

    @Override
    @Transactional
    public ReportScheduleResponse resumeSchedule(UUID scheduleId) {
        ReportSchedule schedule = reportScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        schedule.setStatus(ReportSchedule.ScheduleStatus.ACTIVE);
        schedule.setNextRunAt(calculateNextRun(buildScheduleRequest(schedule)));
        ReportSchedule saved = reportScheduleRepository.save(schedule);
        return mapScheduleToResponse(saved);
    }

    @Override
    @Transactional
    public ReportScheduleResponse deleteSchedule(UUID scheduleId) {
        ReportSchedule schedule = reportScheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found"));
        
        schedule.setStatus(ReportSchedule.ScheduleStatus.STOPPED);
        reportScheduleRepository.save(schedule);
        
        return mapScheduleToResponse(schedule);
    }

    private byte[] generateReportData(Report report) {
        StringBuilder data = new StringBuilder();
        
        switch (report.getFormat()) {
            case CSV:
                data.append("Report Type,Date Range,Generated At\n");
                data.append(String.format("%s,%s - %s,%s\n",
                        report.getReportType().getDisplayName(),
                        report.getStartDate(),
                        report.getEndDate(),
                        LocalDateTime.now()));
                break;
            case PDF:
            case EXCEL:
                data.append("Report: ").append(report.getTitle()).append("\n");
                data.append("Type: ").append(report.getReportType().getDisplayName()).append("\n");
                data.append("Generated: ").append(LocalDateTime.now()).append("\n");
                break;
        }
        
        return data.toString().getBytes();
    }

    private String buildCronExpression(ReportScheduleRequest request) {
        return String.format("0 %d %d ? * %s",
                request.getMinute(),
                request.getHour(),
                request.getScheduleType() == ReportScheduleRequest.ScheduleType.WEEKLY 
                        ? request.getDayOfWeek().name() 
                        : "*");
    }

    private LocalDateTime calculateNextRun(ReportScheduleRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.withHour(request.getHour()).withMinute(request.getMinute()).withSecond(0);
        
        if (next.isBefore(now)) {
            switch (request.getScheduleType()) {
                case DAILY:
                    next = next.plusDays(1);
                    break;
                case WEEKLY:
                    next = next.plusWeeks(1);
                    if (request.getDayOfWeek() != null) {
                        next = now.plusWeeks(1).with(request.getDayOfWeek());
                    }
                    break;
                case MONTHLY:
                    next = next.plusMonths(1);
                    if (request.getDayOfMonth() != null) {
                        next = now.plusMonths(1).withDayOfMonth(request.getDayOfMonth());
                    }
                    break;
            }
        }
        
        return next;
    }

    private ReportScheduleRequest buildScheduleRequest(ReportSchedule schedule) {
        return ReportScheduleRequest.builder()
                .scheduleName(schedule.getScheduleName())
                .reportType(schedule.getReportType())
                .scheduleType(ReportScheduleRequest.ScheduleType.valueOf(schedule.getScheduleType().name()))
                .format(schedule.getFormat())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfMonth(schedule.getDayOfMonth())
                .hour(schedule.getHour())
                .minute(schedule.getMinute())
                .emailRecipients(schedule.getRecipients() != null 
                        ? Arrays.asList(schedule.getRecipients().split(",")) 
                        : Collections.emptyList())
                .build();
    }

    private String serializeFilters(List<String> filters) {
        if (filters == null || filters.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> deserializeFilters(String filters) {
        if (filters == null || filters.isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(filters, List.class);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private ReportResponse mapToResponse(Report report) {
        return ReportResponse.builder()
                .id(report.getId())
                .reportNumber(report.getReportNumber())
                .reportType(report.getReportType())
                .reportTypeDisplayName(report.getReportType().getDisplayName())
                .title(report.getTitle())
                .description(report.getDescription())
                .format(report.getFormat())
                .status(report.getStatus().name())
                .filePath(report.getFilePath())
                .fileSize(report.getFileSize())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .createdBy(report.getCreatedBy())
                .completedAt(report.getCompletedAt())
                .errorMessage(report.getErrorMessage())
                .downloadUrl(report.getStatus() == Report.ReportStatus.COMPLETED 
                        ? "/api/v1/admin/reports/" + report.getId() + "/download" 
                        : null)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }

    private ReportScheduleResponse mapScheduleToResponse(ReportSchedule schedule) {
        List<String> recipients = schedule.getRecipients() != null && !schedule.getRecipients().isEmpty()
                ? Arrays.asList(schedule.getRecipients().split(","))
                : Collections.emptyList();
        
        return ReportScheduleResponse.builder()
                .id(schedule.getId())
                .scheduleName(schedule.getScheduleName())
                .reportType(schedule.getReportType())
                .reportTypeDisplayName(schedule.getReportType().getDisplayName())
                .scheduleType(schedule.getScheduleType().name())
                .format(schedule.getFormat())
                .status(schedule.getStatus().name())
                .cronExpression(schedule.getCronExpression())
                .dayOfWeek(schedule.getDayOfWeek())
                .dayOfMonth(schedule.getDayOfMonth())
                .hour(schedule.getHour())
                .minute(schedule.getMinute())
                .startDate(schedule.getStartDate())
                .endDate(schedule.getEndDate())
                .lastRunAt(schedule.getLastRunAt())
                .nextRunAt(schedule.getNextRunAt())
                .recipients(recipients)
                .createdBy(schedule.getCreatedBy())
                .createdAt(schedule.getCreatedAt())
                .updatedAt(schedule.getUpdatedAt())
                .build();
    }
}
