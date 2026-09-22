package ecommerce.modules.shipping.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class CreateFulfillmentRequest {

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Seller order ID is required")
    private UUID sellerOrderId;

    private String notes;
}
