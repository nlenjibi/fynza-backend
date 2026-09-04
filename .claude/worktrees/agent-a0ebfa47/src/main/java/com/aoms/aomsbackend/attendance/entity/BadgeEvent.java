package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "badge_events",
    indexes = {
        @Index(name = "idx_badge_events_user_id", columnList = "user_id"),
        @Index(name = "idx_badge_events_building_id", columnList = "building_id"),
        @Index(name = "idx_badge_events_event_type", columnList = "event_type"),
        @Index(name = "idx_badge_events_occurred_at", columnList = "occurred_at")
    }
)
public class BadgeEvent {

    // Badge events are ingested externally; IDs are always set by the upstream system.
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "personnel_id", length = 100)
    private String personnelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 20)
    private BadgeEventType eventType;

    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;

    @Column(name = "ingested_at", nullable = false)
    private OffsetDateTime ingestedAt;

    @Column(name = "is_resolved", nullable = false)
    private boolean resolved;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_job_run_id")
    private UUID resolutionJobRunId;

    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "job_run_id")
    private UUID jobRunId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
