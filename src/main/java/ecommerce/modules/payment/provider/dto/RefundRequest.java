package ecommerce.modules.payment.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RefundRequest {
    String reference;
    BigDecimal amount;
    String reason;
}
