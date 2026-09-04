package ecommerce.modules.report.entity;

import ecommerce.common.base.BaseEntity;
import ecommerce.common.enums.ReportFormat;
import ecommerce.common.enums.ReportType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.DayOfWeek;
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
@SuperBuilder
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class ReportSchedule extends BaseEntity {

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

    public enum ScheduleType {
        DAILY, WEEKLY, MONTHLY
    }

    public enum ScheduleStatus {
        ACTIVE, PAUSED, STOPPED
    }
}
