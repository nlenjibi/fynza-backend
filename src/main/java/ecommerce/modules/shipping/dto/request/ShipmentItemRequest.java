package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ShipmentItemRequest {

    @NotNull(message = "Order item ID is required")
    private UUID orderItemId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    private UUID variantId;

    @NotBlank(message = "Product name is required")
    private String productName;

    private String productSku;

    @NotNull
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
}
