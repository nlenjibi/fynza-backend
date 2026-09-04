package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

/**
 * Minimal read-only JPA mapping for the {@code room} table.
 * Used to resolve building context from a room ID.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "room")
public class Room {

    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "floor_id", nullable = false)
    private UUID floorId;

    @Column(name = "room_name", nullable = false, length = 100)
    private String roomName;

    @Column(name = "room_type", nullable = false, length = 30)
    private String roomType;

    @Column(name = "total_seats")
    private Integer totalSeats;

    @Column(name = "is_active", nullable = false)
    private boolean active;
}
