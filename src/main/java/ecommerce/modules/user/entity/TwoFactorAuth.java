package ecommerce.modules.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "two_factor_auth", indexes = {
    @Index(name = "idx_2fa_user", columnList = "user_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "secret_key", nullable = false, length = 255)
    private String secretKey;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = false;

    @Column(name = "enabled_at")
    private LocalDateTime enabledAt;

    @Column(name = "authenticator_type", length = 20)
    @Builder.Default
    private String authenticatorType = "TOTP";

    @Column(name = "recovery_codes", columnDefinition = "TEXT")
    private String recoveryCodes;

    @Column(name = "backup_codes_used", nullable = false)
    @Builder.Default
    private Integer backupCodesUsed = 0;

    @Column(name = "last_verified_at")
    private LocalDateTime lastVerifiedAt;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
}
