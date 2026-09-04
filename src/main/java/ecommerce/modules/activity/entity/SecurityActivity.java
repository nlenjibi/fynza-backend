package ecommerce.modules.activity.entity;

import ecommerce.common.base.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "security_activities", indexes = {
    @Index(name = "idx_security_user", columnList = "user_id"),
    @Index(name = "idx_security_type", columnList = "activity_type"),
    @Index(name = "idx_security_created", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class SecurityActivity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 50)
    private SecurityActivityType activityType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    public enum SecurityActivityType {
        PASSWORD_CHANGED,
        PASSWORD_CHANGE_FAILED,
        PASSWORD_RESET_REQUESTED,
        PASSWORD_RESET_COMPLETED,
        PASSWORD_RESET_FAILED,
        TWO_FA_ENABLED,
        TWO_FA_DISABLED,
        TWO_FA_SETUP_INITIATED,
        TWO_FA_SETUP_COMPLETED,
        TWO_FA_SETUP_FAILED,
        TWO_FA_VERIFICATION_SUCCESS,
        TWO_FA_VERIFICATION_FAILED,
        TWO_FA_BACKUP_CODES_GENERATED,
        TWO_FA_BACKUP_CODES_USED,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        ACCOUNT_LOCKED,
        ACCOUNT_UNLOCKED,
        SESSION_EXPIRED,
        EMAIL_CHANGED,
        PHONE_CHANGED,
        PROFILE_UPDATED,
        SUSPICIOUS_ACTIVITY_DETECTED
    }
}
