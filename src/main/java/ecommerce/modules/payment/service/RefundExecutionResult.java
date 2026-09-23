package ecommerce.modules.payment.service;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class RefundExecutionResult {
    boolean success;
    String providerRefundId;
    String providerReference;
    BigDecimal refundedAmount;
    String failureReason;
}
