package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "job_execution_log", 
    indexes = {
        @Index(name = "idx_job_execution_log_lookup", columnList = "job_name, location_id, target_date, status"),
        @Index(name = "idx_job_execution_log_run_id", columnList = "run_id")
    }
)
@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceStampLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    @Column(name = "location_id")
    private UUID locationId;

    @Column(name = "target_date", nullable = false)
    private LocalDate targetDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "records_skipped")
    private Integer recordsSkipped;

    @Column(name = "records_failed")
    private Integer recordsFailed;

    @Column(name = "records_released")
    private Integer recordsReleased;

    @Column(name = "run_id")
    private UUID runId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = LocalDateTime.now();
        }
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AttendanceStampLog that = (AttendanceStampLog) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}