package ecommerce.modules.inventory.dto.response;

import ecommerce.modules.inventory.enums.InventoryStatus;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AvailabilityResponse {

    private UUID productId;
    private UUID variantId;
    private int availableQuantity;
    private InventoryStatus status;
    private boolean allowBackorder;
}
