package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only JPA mapping for the {@code office_building} table.
 * Used as a FK reference from {@link Employee}, {@link WorkSession}, and {@link AttendanceRecord}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "office_building")
public class OfficeBuilding {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "office_id", nullable = false)
    private UUID officeId;

    @Column(name = "building_name", nullable = false)
    private String buildingName;

    @Column(name = "address")
    private String address;

    @Column(name = "total_floors")
    private Integer totalFloors;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
