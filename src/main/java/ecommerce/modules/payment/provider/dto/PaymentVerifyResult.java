package ecommerce.modules.payment.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value
@Builder
public class PaymentVerifyResult {
    String reference;
    String status;
    BigDecimal amount;
    String currency;
    String providerTransactionId;
    String channel;
    String gatewayResponse;
    Instant paidAt;
}
