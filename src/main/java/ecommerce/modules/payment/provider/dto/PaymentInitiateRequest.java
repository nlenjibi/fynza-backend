package ecommerce.modules.payment.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class PaymentInitiateRequest {
    BigDecimal amount;
    String currency;
    String email;
    String reference;
    String callbackUrl;
    String metadata;
    String description;
}
