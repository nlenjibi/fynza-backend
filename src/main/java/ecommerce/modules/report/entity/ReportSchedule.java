package ecommerce.modules.report.entity;

import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "report_schedules", indexes = {
        @Index(name = "idx_report_schedule_type", columnList = "report_type"),
        @Index(name = "idx_report_schedule_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ReportSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @EqualsAndHashCode.Include
    @Column(name = "schedule_name", nullable = false)
    private String scheduleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 30)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "schedule_type", nullable = false, length = 20)
    private ScheduleType scheduleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 20)
    private ReportFormat format;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.ACTIVE;

    @Column(name = "cron_expression")
    private String cronExpression;

    @Column(name = "day_of_week")
    @Enumerated(EnumType.STRING)
    private DayOfWeek dayOfWeek;

    @Column(name = "day_of_month")
    private Integer dayOfMonth;

    @Column(name = "hour")
    private Integer hour;

    @Column(name = "minute")
    private Integer minute;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "recipients", columnDefinition = "TEXT")
    private String recipients;

    @Column(name = "created_by", nullable = false, columnDefinition = "UUID")
    private UUID createdBy;

    @Column(name = "filters", columnDefinition = "TEXT")
    private String filters;

    @PrePersist
    protected void onCreate() {
        publicId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum ScheduleType {
        DAILY, WEEKLY, MONTHLY
    }

    public enum ScheduleStatus {
        ACTIVE, PAUSED, STOPPED
    }
}
