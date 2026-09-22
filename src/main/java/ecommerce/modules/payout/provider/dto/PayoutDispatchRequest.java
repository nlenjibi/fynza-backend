package ecommerce.modules.payout.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder
public class PayoutDispatchRequest {
    String idempotencyKey;
    String accountIdentifier;
    String bankCode;
    String accountName;
    BigDecimal amount;
    String currency;
    String narration;
    UUID payoutPublicId;
}
