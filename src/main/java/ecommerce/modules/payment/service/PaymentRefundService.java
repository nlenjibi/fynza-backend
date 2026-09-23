package ecommerce.modules.payment.service;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentRefundService {

    /**
     * Execute a refund against the payment provider for the given order.
     * Finds the original completed payment transaction, dispatches to the correct
     * provider, and returns the execution result. Never throws — failures are
     * surfaced as RefundExecutionResult.success = false.
     */
    RefundExecutionResult executeRefund(UUID orderId, BigDecimal amount, String reason, String idempotencyKey);
}
