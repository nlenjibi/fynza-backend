package ecommerce.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "inventory_transfer_items", indexes = {
        @Index(name = "idx_inv_transfer_items_transfer_id",  columnList = "transfer_id"),
        @Index(name = "idx_inv_transfer_items_inventory_id", columnList = "inventory_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class InventoryTransferItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "transfer_id", nullable = false, updatable = false)
    private Long transferId;

    @Column(name = "inventory_id", nullable = false, updatable = false)
    private Long inventoryId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "received_quantity", nullable = false)
    @Builder.Default
    private int receivedQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
