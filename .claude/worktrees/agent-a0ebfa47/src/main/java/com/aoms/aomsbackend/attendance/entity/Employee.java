package com.aoms.aomsbackend.attendance.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Read-only JPA mapping for the {@code employee} table managed by the data-engineering pipeline.
 * The {@code manager_id} self-reference defines the direct-report relationship used for team attendance scoping.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "employee")
public class Employee {
    @Id
    @Column(name = "id")
    private UUID id;

    @Column(name = "employee_code")
    private String employeeCode;

    @Column(name = "sso_user_id")
    private String ssoUserId;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "primary_building_id", nullable = false)
    private UUID primaryBuildingId;

    @Column(name = "manager_id")
    private UUID managerId;

    @Column(name = "employee_type")
    private String employeeType;

    @Column(name = "department")
    private String department;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "project")
    private String project;

    @Column(name = "language_preference")
    private String languagePreference;

    @Column(name = "employment_start_date", nullable = false)
    private LocalDate employmentStartDate;

    @Column(name = "employment_end_date")
    private LocalDate employmentEndDate;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** Returns the employee's full name as "{firstName} {lastName}". */
    public String getDisplayName() {
        return firstName + " " + lastName;
    }
}
