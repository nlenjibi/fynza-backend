package ecommerce.modules.user.entity;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_email",    columnList = "email"),
        @Index(name = "idx_users_username", columnList = "username"),
        @Index(name = "idx_users_role",     columnList = "role"),
        @Index(name = "idx_users_status",   columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id", insertable = false, updatable = false)
    private UUID publicId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) id = UUID.randomUUID();
        createdAt  = Instant.now();
        updatedAt  = Instant.now();
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getPublicId() { return publicId != null ? publicId : id; }

    // ── Identity ─────────────────────────────────────────────────────────────

    @Column(name = "email", unique = true, nullable = false, length = 255)
    private String email;

    @Column(name = "username", unique = true, nullable = false, length = 100)
    private String username;

    @Column(name = "password", nullable = false)
    private String password;

    // ── Personal information ──────────────────────────────────────────────────

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "display_name", length = 100)
    private String displayName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    // ── Preferences ───────────────────────────────────────────────────────────

    @Column(name = "language", length = 10)
    @Builder.Default
    private String language = "en";

    @Column(name = "timezone", length = 50)
    @Builder.Default
    private String timezone = "UTC";

    @Column(name = "currency", length = 3)
    @Builder.Default
    private String currency = "GHS";

    // ── Authorization ─────────────────────────────────────────────────────────

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    @Builder.Default
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean isEmailVerified = false;

    @Column(name = "is_locked")
    @Builder.Default
    private Boolean isLocked = false;

    // ── Account lifecycle ─────────────────────────────────────────────────────

    @Column(name = "suspend_reason")
    private String suspendReason;

    @Column(name = "suspended_by")
    private UUID suspendedBy;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "deletion_requested_at")
    private Instant deletionRequestedAt;

    @Column(name = "scheduled_deletion_at")
    private Instant scheduledDeletionAt;

    // ── Auth tracking ─────────────────────────────────────────────────────────

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "last_password_change")
    private LocalDateTime lastPasswordChange;

    @Column(name = "mfa_enabled", nullable = false)
    @Builder.Default
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 64)
    private String mfaSecret;

    // ── OAuth ─────────────────────────────────────────────────────────────────

    @Column(name = "google_id")
    private String googleId;

    @Column(name = "github_id")
    private String githubId;

    @Column(name = "oauth_provider")
    private String oauthProvider;

    // ── Helpers ───────────────────────────────────────────────────────────────

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public String getEffectiveDisplayName() {
        return displayName != null ? displayName : getFullName();
    }
}
