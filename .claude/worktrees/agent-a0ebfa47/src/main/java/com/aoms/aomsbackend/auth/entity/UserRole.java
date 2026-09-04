package com.aoms.aomsbackend.auth.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

/**
 * The type User role.
 */
@Entity
@Table(name = "user_role", indexes = {
    @Index(name = "idx_user_role_user_id", columnList = "user_id"),
    @Index(name = "idx_user_role_organisation_id", columnList = "organisation_id"),
    @Index(name = "idx_user_role_role", columnList = "role"),
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRole implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private UserRoleType role;

    @Column(name = "organisation_id")
    private UUID organisationId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "assigned_by")
    private UUID assignedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    /**
     * On create.
     */
    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.assignedAt = now;
        this.createdAt = now;
    }


}
