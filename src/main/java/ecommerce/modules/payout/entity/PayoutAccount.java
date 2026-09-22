package ecommerce.modules.payout.entity;

import ecommerce.modules.payout.enums.PayoutAccountStatus;
import ecommerce.modules.payout.enums.PayoutAccountType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payout_accounts", indexes = {
        @Index(name = "idx_payout_accounts_seller_id",  columnList = "seller_id"),
        @Index(name = "idx_payout_accounts_status",     columnList = "status"),
        @Index(name = "idx_payout_accounts_is_active",  columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PayoutAccountType type;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(name = "account_identifier", nullable = false, length = 255)
    private String accountIdentifier;

    @Column(name = "bank_code", length = 20)
    private String bankCode;

    @Column(name = "country", nullable = false, length = 3)
    @Builder.Default
    private String country = "GH";

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "GHS";

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @Column(name = "is_verified", nullable = false)
    @Builder.Default
    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PayoutAccountStatus status = PayoutAccountStatus.PENDING;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
