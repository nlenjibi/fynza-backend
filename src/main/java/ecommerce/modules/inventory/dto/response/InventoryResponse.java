package ecommerce.modules.inventory.dto.response;

import ecommerce.modules.inventory.entity.Inventory;
import ecommerce.modules.inventory.enums.InventoryStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryResponse {

    private UUID publicId;
    private UUID productId;
    private UUID variantId;
    private Long sellerId;
    private Long storeId;
    private Long locationId;
    private int onHandQuantity;
    private int reservedQuantity;
    private int availableQuantity;
    private int incomingQuantity;
    private int damagedQuantity;
    private int lowStockThreshold;
    private boolean allowBackorder;
    private InventoryStatus status;
    private Long version;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;

    public static InventoryResponse from(Inventory inv) {
        return InventoryResponse.builder()
                .publicId(inv.getPublicId())
                .productId(inv.getProductId())
                .variantId(inv.getVariantId())
                .sellerId(inv.getSellerId())
                .storeId(inv.getStoreId())
                .locationId(inv.getLocationId())
                .onHandQuantity(inv.getOnHandQuantity())
                .reservedQuantity(inv.getReservedQuantity())
                .availableQuantity(inv.getAvailableQuantity())
                .incomingQuantity(inv.getIncomingQuantity())
                .damagedQuantity(inv.getDamagedQuantity())
                .lowStockThreshold(inv.getLowStockThreshold())
                .allowBackorder(inv.isAllowBackorder())
                .status(inv.getStatus())
                .version(inv.getVersion())
                .isActive(inv.getIsActive())
                .createdAt(inv.getCreatedAt())
                .updatedAt(inv.getUpdatedAt())
                .build();
    }
}
