package ecommerce.modules.payment.provider.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PaymentVerifyRequest {
    String reference;
}
