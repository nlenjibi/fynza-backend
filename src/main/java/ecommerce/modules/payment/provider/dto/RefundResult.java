package ecommerce.modules.payment.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RefundResult {
    String refundId;
    String reference;
    BigDecimal amount;
    String currency;
    String status;
}
