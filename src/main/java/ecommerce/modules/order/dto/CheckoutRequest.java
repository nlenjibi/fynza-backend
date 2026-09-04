package ecommerce.modules.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    private UUID shippingAddressId;
    private UUID billingAddressId;
    private String paymentMethod;
    private String paymentIntentId;
    private String couponCode;
}
