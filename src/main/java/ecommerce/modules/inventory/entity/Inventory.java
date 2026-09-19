package ecommerce.modules.inventory.entity;

import ecommerce.modules.inventory.enums.InventoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "inventory", indexes = {
        @Index(name = "idx_inventory_product_id",  columnList = "product_id"),
        @Index(name = "idx_inventory_variant_id",  columnList = "variant_id"),
        @Index(name = "idx_inventory_seller_id",   columnList = "seller_id"),
        @Index(name = "idx_inventory_location_id", columnList = "location_id"),
        @Index(name = "idx_inventory_is_active",   columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private UUID productId;

    @Column(name = "variant_id", updatable = false)
    private UUID variantId;

    @Column(name = "seller_id", nullable = false, updatable = false)
    private Long sellerId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "location_id", nullable = false, updatable = false)
    private Long locationId;

    @Column(name = "on_hand_quantity", nullable = false)
    @Builder.Default
    private int onHandQuantity = 0;

    @Column(name = "reserved_quantity", nullable = false)
    @Builder.Default
    private int reservedQuantity = 0;

    @Column(name = "incoming_quantity", nullable = false)
    @Builder.Default
    private int incomingQuantity = 0;

    @Column(name = "damaged_quantity", nullable = false)
    @Builder.Default
    private int damagedQuantity = 0;

    @Column(name = "low_stock_threshold", nullable = false)
    @Builder.Default
    private int lowStockThreshold = 5;

    @Column(name = "allow_backorder", nullable = false)
    @Builder.Default
    private boolean allowBackorder = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

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
        if (isActive == null) isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public int getAvailableQuantity() {
        return (int) Math.max(0L, (long) onHandQuantity - reservedQuantity);
    }

    public InventoryStatus getStatus() {
        if (!Boolean.TRUE.equals(isActive)) return InventoryStatus.DISABLED;
        int available = getAvailableQuantity();
        if (available == 0) return InventoryStatus.OUT_OF_STOCK;
        if (available <= lowStockThreshold) return InventoryStatus.LOW_STOCK;
        return InventoryStatus.IN_STOCK;
    }
}
