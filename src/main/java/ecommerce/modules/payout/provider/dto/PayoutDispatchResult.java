package ecommerce.modules.payout.provider.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PayoutDispatchResult {
    boolean success;
    String providerReference;
    String status;
    String message;
}
