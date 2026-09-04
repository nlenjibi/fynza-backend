package com.aoms.aomsbackend.seating.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seat", indexes = {
        @Index(name = "idx_seat_zone_id", columnList = "zone_id"),
        @Index(name = "idx_seat_is_active", columnList = "is_active"),
        @Index(name = "idx_seat_room_id", columnList = "room_id"),
        @Index(name = "idx_seat_floor_id", columnList = "floor_id"),
        @Index(name = "idx_seat_building_id", columnList = "building_id"),
        @Index(name = "idx_seat_assigned_employee_id", columnList = "assigned_employee_id"),
        @Index(name = "idx_seat_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "zone_id")
    private UUID zoneId;

    @Column(name = "seat_number", length = 20)
    private String seatNumber;

    @Column(name = "seat_label", length = 50)
    private String seatLabel;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 20)
    private SeatType seatType;

    @Column(name = "permanent_user_id")
    private UUID permanentUserId;
    @Column(name = "room_id")
    private UUID roomId;

    @Column(name = "floor_id", nullable = false)
    private UUID floorId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private SeatStatus status = SeatStatus.AVAILABLE;

    @Column(name = "assigned_employee_id")
    private UUID assignedEmployeeId;

    @Column(name = "x_position")
    private Float xPosition;

    @Column(name = "y_position")
    private Float yPosition;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;


    public boolean isBookable() {
        return status == SeatStatus.AVAILABLE;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}