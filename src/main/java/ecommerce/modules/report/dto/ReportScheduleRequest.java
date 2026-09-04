package ecommerce.modules.report.dto;

import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportScheduleRequest {

    @NotBlank(message = "Schedule name is required")
    private String scheduleName;

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Schedule type is required")
    private ReportScheduleRequest.ScheduleType scheduleType;

    @NotNull(message = "Format is required")
    private ReportFormat format;

    private DayOfWeek dayOfWeek;

    private Integer dayOfMonth;

    @NotNull(message = "Hour is required")
    private Integer hour;

    @NotNull(message = "Minute is required")
    private Integer minute;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<String> emailRecipients;

    private List<String> filters;

    public enum ScheduleType {
        DAILY, WEEKLY, MONTHLY
    }
}
