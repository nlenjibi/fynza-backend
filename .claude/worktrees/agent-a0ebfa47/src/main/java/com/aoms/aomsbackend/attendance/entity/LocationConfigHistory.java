package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "location_config_history", indexes = {
        @Index(name = "idx_location_config_history_building_id", columnList = "building_id"),
        @Index(name = "idx_location_config_history_changed_at", columnList = "changed_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationConfigHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_mode", length = 20)
    private SeatVisibilityMode previousMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_mode", nullable = false, length = 20)
    private SeatVisibilityMode newMode;

    @Column(name = "changed_by", nullable = false)
    private UUID changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    @PrePersist
    protected void onCreate() {
        changedAt = Instant.now();
    }
}
