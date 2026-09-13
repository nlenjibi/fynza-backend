package ecommerce.modules.inventory.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateInventoryRequest {

    @NotNull
    private UUID productId;

    private UUID variantId;

    @NotNull
    private Long locationId;

    @Min(0)
    private int initialQuantity = 0;

    @Min(0)
    private int lowStockThreshold = 5;

    private boolean allowBackorder = false;

    private Long storeId;
}
