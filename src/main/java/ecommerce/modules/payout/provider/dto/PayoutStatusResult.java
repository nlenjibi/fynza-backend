package ecommerce.modules.payout.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PayoutStatusResult {
    String providerReference;
    String status;
    String failureReason;
    Instant updatedAt;
}
