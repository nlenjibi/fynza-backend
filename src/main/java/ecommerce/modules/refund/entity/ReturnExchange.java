package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ExchangeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_exchanges", indexes = {
        @Index(name = "idx_return_exchanges_return_id",  columnList = "return_id"),
        @Index(name = "idx_return_exchanges_status",     columnList = "status"),
        @Index(name = "idx_return_exchanges_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnExchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "return_id", nullable = false, unique = true)
    private UUID returnId;

    @Column(name = "original_order_id", nullable = false)
    private UUID originalOrderId;

    /** Populated when the exchange fulfilment order is created. */
    @Column(name = "exchange_order_id")
    private UUID exchangeOrderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ExchangeStatus status = ExchangeStatus.PENDING;

    @Column(name = "requested_items_description", columnDefinition = "TEXT")
    private String requestedItemsDescription;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "requested_by", nullable = false)
    private UUID requestedBy;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
