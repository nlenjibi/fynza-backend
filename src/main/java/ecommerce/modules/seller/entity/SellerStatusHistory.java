package ecommerce.modules.seller.entity;

import ecommerce.common.enums.SellerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "seller_status_history", indexes = {
        @Index(name = "idx_seller_status_hist_seller", columnList = "seller_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SellerStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private SellerStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 30)
    private SellerStatus newStatus;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "changed_by")
    private UUID changedBy;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
