package ecommerce.modules.payment.provider.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class PaymentInitiateResult {
    String reference;
    String authorizationUrl;
    String accessCode;
    String providerTransactionId;
    Instant expiresAt;
}
