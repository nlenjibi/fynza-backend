package ecommerce.modules.admin.async;

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
public class ReportResult {

    private UUID reportId;
    private String reportName;
    private ReportStatus status;
    private int progressPercentage;
    private String downloadUrl;
    private LocalDateTime completedAt;
    private String errorMessage;

    public enum ReportStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public static ReportResult processing(UUID reportId, String name) {
        return ReportResult.builder()
                .reportId(reportId)
                .reportName(name)
                .status(ReportStatus.PROCESSING)
                .progressPercentage(0)
                .build();
    }
}
