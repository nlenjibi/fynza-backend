package ecommerce.modules.report.dto;

import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportScheduleResponse {

    private UUID id;
    private String scheduleName;
    private ReportType reportType;
    private String reportTypeDisplayName;
    private String scheduleType;
    private ReportFormat format;
    private String status;
    private String cronExpression;
    private DayOfWeek dayOfWeek;
    private Integer dayOfMonth;
    private Integer hour;
    private Integer minute;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private List<String> recipients;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
