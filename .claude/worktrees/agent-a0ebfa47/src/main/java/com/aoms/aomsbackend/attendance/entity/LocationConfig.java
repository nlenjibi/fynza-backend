package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "location_config")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "building_id", nullable = false, updatable = false)
    private UUID buildingId;

    @Column(name = "work_start_time", nullable = false)
    private LocalTime workStartTime;

    @Column(name = "lateness_threshold_minutes", nullable = false)
    private Integer latenessThresholdMinutes;

    @Column(name = "min_presence_duration_minutes", nullable = false)
    private Integer minPresenceDurationMinutes;

    @Column(name = "no_show_release_time")
    private LocalTime noShowReleaseTime;

    @Column(name = "hot_desk_booking_window_days")
    private Integer hotDeskBookingWindowDays;

    @Column(name = "booking_cancellation_cutoff_hours")
    private Integer bookingCancellationCutoffHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_visibility_mode", length = 20)
    private SeatVisibilityMode seatVisibilityMode;

    @Column(name = "session_gap_threshold_hours")
    private Integer sessionGapThresholdHours;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
