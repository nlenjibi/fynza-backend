package ecommerce.modules.inventory.dto.response;

import ecommerce.modules.inventory.entity.InventoryLocation;
import ecommerce.modules.inventory.enums.LocationType;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class InventoryLocationResponse {

    private UUID publicId;
    private Long sellerId;
    private Long storeId;
    private String name;
    private String code;
    private LocationType locationType;
    private String status;
    private Boolean isActive;
    private Instant createdAt;

    public static InventoryLocationResponse from(InventoryLocation loc) {
        return InventoryLocationResponse.builder()
                .publicId(loc.getPublicId())
                .sellerId(loc.getSellerId())
                .storeId(loc.getStoreId())
                .name(loc.getName())
                .code(loc.getCode())
                .locationType(loc.getLocationType())
                .status(loc.getStatus())
                .isActive(loc.getIsActive())
                .createdAt(loc.getCreatedAt())
                .build();
    }
}
