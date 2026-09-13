package ecommerce.modules.inventory.entity;

import ecommerce.modules.inventory.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory_transfers", indexes = {
        @Index(name = "idx_inv_transfers_source_location",      columnList = "source_location_id"),
        @Index(name = "idx_inv_transfers_destination_location", columnList = "destination_location_id"),
        @Index(name = "idx_inv_transfers_status",               columnList = "status"),
        @Index(name = "idx_inv_transfers_requested_by",         columnList = "requested_by")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InventoryTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "source_location_id", nullable = false, updatable = false)
    private Long sourceLocationId;

    @Column(name = "destination_location_id", nullable = false, updatable = false)
    private Long destinationLocationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private TransferStatus status = TransferStatus.REQUESTED;

    @Column(name = "requested_by", nullable = false, updatable = false)
    private UUID requestedBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = TransferStatus.REQUESTED;
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
