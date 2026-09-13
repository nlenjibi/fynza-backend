package ecommerce.modules.inventory.entity;

import ecommerce.modules.inventory.enums.MovementType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_movements", indexes = {
        @Index(name = "idx_stock_movements_inventory_id",  columnList = "inventory_id"),
        @Index(name = "idx_stock_movements_movement_type", columnList = "movement_type"),
        @Index(name = "idx_stock_movements_created_at",    columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "inventory_id", nullable = false, updatable = false)
    private Long inventoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 30, updatable = false)
    private MovementType movementType;

    @Column(name = "quantity", nullable = false, updatable = false)
    private int quantity;

    @Column(name = "previous_quantity", nullable = false, updatable = false)
    private int previousQuantity;

    @Column(name = "new_quantity", nullable = false, updatable = false)
    private int newQuantity;

    @Column(name = "reference_type", length = 50, updatable = false)
    private String referenceType;

    @Column(name = "reference_id", updatable = false)
    private UUID referenceId;

    @Column(name = "reason", updatable = false)
    private String reason;

    @Column(name = "performed_by", updatable = false)
    private UUID performedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
