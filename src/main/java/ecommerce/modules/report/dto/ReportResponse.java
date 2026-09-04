package ecommerce.modules.report.dto;

import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    private UUID id;
    private String reportNumber;
    private ReportType reportType;
    private String reportTypeDisplayName;
    private String title;
    private String description;
    private ReportFormat format;
    private String status;
    private String filePath;
    private Long fileSize;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private UUID createdBy;
    private LocalDateTime completedAt;
    private String errorMessage;
    private String downloadUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
