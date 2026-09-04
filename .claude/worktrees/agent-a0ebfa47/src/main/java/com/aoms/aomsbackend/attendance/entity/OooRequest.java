package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ooo_request",
    indexes = {
        @Index(name = "idx_ooo_request_employee_id",  columnList = "employee_id"),
        @Index(name = "idx_ooo_request_building_id",  columnList = "building_id"),
        @Index(name = "idx_ooo_request_status",       columnList = "status"),
        @Index(name = "idx_ooo_request_dates",        columnList = "start_date, end_date")
    }
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OooRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "building_id", nullable = false)
    private UUID buildingId;

    @Column(name = "ooo_type", nullable = false, length = 30)
    private String oooType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
