package com.aoms.aomsbackend.seating.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "seat_booking", indexes = {
    @Index(name = "idx_seat_booking_user_id",     columnList = "user_id"),
    @Index(name = "idx_seat_booking_seat_id",     columnList = "seat_id"),
    @Index(name = "idx_seat_booking_date",        columnList = "booking_date"),
    @Index(name = "idx_seat_booking_building_id", columnList = "building_id")
})
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeatBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "seat_id", nullable = false)
    private UUID seatId;

    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SeatBookingStatus status;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "block_reservation_id")
    private UUID blockReservationId;

    @Setter
    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Setter
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Setter
    @Column(name = "auto_released_at")
    private OffsetDateTime autoReleasedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
