package ecommerce.modules.report.dto;

import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRequest {

    @NotNull(message = "Report type is required")
    private ReportType reportType;

    @NotNull(message = "Format is required")
    private ReportFormat format;

    private String title;

    private String description;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    private List<String> filters;

    private List<String> emailRecipients;
}
