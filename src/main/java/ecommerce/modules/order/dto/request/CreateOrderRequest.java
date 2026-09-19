package ecommerce.modules.order.dto.request;

import ecommerce.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {

    @NotNull(message = "Cart ID is required")
    private UUID cartId;

    private UUID shippingAddressId;
    private UUID billingAddressId;

    private PaymentMethod paymentMethod;
    private String couponCode;
    private String customerNotes;
    private String idempotencyKey;
}
