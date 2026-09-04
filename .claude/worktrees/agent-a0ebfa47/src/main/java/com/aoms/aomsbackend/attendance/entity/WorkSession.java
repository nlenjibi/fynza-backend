package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only JPA mapping for the {@code work_session} table managed by the data-engineering pipeline.
 * Represents a resolved badge-in/out session for a single employee on a given date.
 * Split sessions (gap-based) are distinguished by {@code sessionSplitIndex}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "work_session")
public class WorkSession {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "first_badge_in")
    private Instant firstBadgeIn;

    @Column(name = "last_badge_out")
    private Instant lastBadgeOut;

    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(name = "is_late")
    private Boolean isLate;

    @Column(name = "session_split_count")
    private Integer sessionSplitCount;

    @Column(name = "session_split_index", nullable = false)
    private Integer sessionSplitIndex;

    @Column(name = "minutes_late")
    private Integer minutesLate;

    @Column(name = "crosses_midnight")
    private Boolean crossesMidnight;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
