package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only JPA mapping for the {@code attendance_record} table managed by the data-engineering pipeline.
 * Each record represents one employee's attendance status for a single date at a building.
 * Optionally linked to a {@link WorkSession} for badge-in/out timing data.
 */
@Entity
@Table(name = "attendance_record",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"user_id", "record_date", "building_id"})
        },
        indexes = {
                @Index(name = "idx_attendance_record_user_id", columnList = "user_id"),
                @Index(name = "idx_attendance_record_office_id", columnList = "office_id"),
                @Index(name = "idx_attendance_record_building_id", columnList = "building_id"),
                @Index(name = "idx_attendance_record_record_date", columnList = "record_date"),
                @Index(name = "idx_attendance_record_status", columnList = "status"),
                @Index(name = "idx_attendance_record_work_session_id", columnList = "work_session_id"),
                @Index(name = "idx_attendance_record_user_date", columnList = "user_id, record_date"),
                @Index(name = "idx_attendance_record_building_date", columnList = "building_id, record_date"),
                @Index(name = "idx_attendance_record_pass_run_id", columnList = "pass_run_id"),
                @Index(name = "idx_attendance_record_is_overridden", columnList = "is_overridden")
        }
)
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AttendanceRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AttendanceStatus status;

    @Column(name = "leave_request_id")
    private UUID leaveRequestId;
    
    @Column(name = "work_session_id", insertable = false, updatable = false)
    private UUID workSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_session_id", insertable = false, updatable = false)
    @ToString.Exclude
    private WorkSession workSession;

    @Column(name = "remote_request_id")
    private UUID remoteRequestId;

    @Column(name = "is_overridden", nullable = false)
    private boolean overridden;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "overridden_at")
    private OffsetDateTime overriddenAt;

    @Column(name = "override_by")
    private UUID overriddenBy;

    @Column(name = "override_reason", columnDefinition = "TEXT")
    private String overrideReason;

    @Column(name = "revert_reasons", columnDefinition = "TEXT")
    private String revertReasons;

    @Column(name = "original_status")
    private String originalStatus;

    @Column(name = "pass_run_id", nullable = false)
    private UUID passRunId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy hibernateProxy
                ? hibernateProxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        AttendanceRecord that = (AttendanceRecord) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        if (this instanceof HibernateProxy hibernateProxy) {
            return hibernateProxy.getHibernateLazyInitializer().getPersistentClass().hashCode();
        }
        return getClass().hashCode();
    }
}
