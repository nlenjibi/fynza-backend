package ecommerce.modules.report.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_type", columnList = "report_type"),
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Report extends BaseEntity {

    @EqualsAndHashCode.Include
    @Column(name = "report_number", nullable = false, unique = true, length = 50)
    private String reportNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private ReportFormat format;

    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "created_by", nullable = false, columnDefinition = "UUID")
    private UUID createdBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters;

    public static String generateReportNumber() {
        return "RPT-" + System.currentTimeMillis();
    }

    public enum ReportStatus {
        PENDING, PROCESSING, COMPLETED, FAILED
    }
}
