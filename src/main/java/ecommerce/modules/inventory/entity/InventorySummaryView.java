package ecommerce.modules.inventory.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_inventory_summary")
@Getter
@NoArgsConstructor
public class InventorySummaryView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "variant_id")
    private UUID variantId;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "on_hand_quantity")
    private Integer onHandQuantity;

    @Column(name = "reserved_quantity")
    private Integer reservedQuantity;

    @Column(name = "incoming_quantity")
    private Integer incomingQuantity;

    @Column(name = "damaged_quantity")
    private Integer damagedQuantity;

    @Column(name = "available_quantity")
    private Integer availableQuantity;

    @Column(name = "low_stock_threshold")
    private Integer lowStockThreshold;

    @Column(name = "allow_backorder")
    private Boolean allowBackorder;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "location_id")
    private Long locationId;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "location_code")
    private String locationCode;

    @Column(name = "location_type")
    private String locationType;

    @Column(name = "location_status")
    private String locationStatus;

    @Column(name = "stock_status")
    private String stockStatus;
}
