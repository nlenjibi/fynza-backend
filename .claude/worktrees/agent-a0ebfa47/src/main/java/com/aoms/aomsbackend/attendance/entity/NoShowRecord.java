package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "no_show_record",
    indexes = {
        @Index(name = "idx_no_show_record_seat_booking_id", columnList = "seat_booking_id"),
        @Index(name = "idx_no_show_record_user_id", columnList = "user_id"),
        @Index(name = "idx_no_show_record_no_show_date", columnList = "no_show_date")
    }
)
public class NoShowRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "seat_booking_id", nullable = false, unique = true)
    private UUID seatBookingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "no_show_date", nullable = false)
    private LocalDate noShowDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
