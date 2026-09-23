package ecommerce.modules.payment.service;

import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.PaymentStatus;
import ecommerce.modules.payment.entity.PaymentTransaction;
import ecommerce.modules.payment.provider.PaymentProvider;
import ecommerce.modules.payment.provider.PaymentProviderType;
import ecommerce.modules.payment.provider.dto.RefundRequest;
import ecommerce.modules.payment.provider.dto.RefundResult;
import ecommerce.modules.payment.repository.PaymentTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentRefundServiceImpl implements PaymentRefundService {

    private final PaymentTransactionRepository transactionRepository;
    private final List<PaymentProvider> providers;

    @Override
    public RefundExecutionResult executeRefund(UUID orderId, BigDecimal amount, String reason, String idempotencyKey) {
        Optional<PaymentTransaction> txOpt =
                transactionRepository.findTopByOrder_PublicIdAndStatusOrderByCreatedAtDesc(orderId, PaymentStatus.COMPLETED);

        if (txOpt.isEmpty()) {
            log.warn("No completed payment transaction found for order={}", orderId);
            return RefundExecutionResult.builder()
                    .success(false)
                    .failureReason("No completed payment transaction found for order: " + orderId)
                    .build();
        }

        PaymentTransaction tx = txOpt.get();
        String reference = tx.getPaystackReference() != null ? tx.getPaystackReference() : tx.getTransactionId();

        PaymentProvider provider = selectProvider(tx);
        if (provider == null) {
            log.warn("No payment provider available for transaction={}", tx.getPublicId());
            return RefundExecutionResult.builder()
                    .success(false)
                    .failureReason("No suitable payment provider found for transaction: " + tx.getPublicId())
                    .build();
        }

        try {
            RefundResult result = provider.refund(RefundRequest.builder()
                    .reference(reference)
                    .amount(amount)
                    .reason(reason)
                    .build());

            log.info("Refund executed via {} for order={}, refundId={}", provider.providerType(), orderId, result.getRefundId());
            return RefundExecutionResult.builder()
                    .success(true)
                    .providerRefundId(result.getRefundId())
                    .providerReference(result.getReference())
                    .refundedAmount(result.getAmount())
                    .build();

        } catch (Exception e) {
            log.error("Provider refund failed for order={}: {}", orderId, e.getMessage(), e);
            return RefundExecutionResult.builder()
                    .success(false)
                    .failureReason(e.getMessage())
                    .build();
        }
    }

    private PaymentProvider selectProvider(PaymentTransaction tx) {
        PaymentProviderType type;
        if (tx.getPaystackReference() != null) {
            type = PaymentProviderType.PAYSTACK;
        } else if (tx.getPaymentMethod() == PaymentMethod.PAYPAL) {
            type = PaymentProviderType.PAYPAL;
        } else if (tx.getPaymentMethod() == PaymentMethod.STRIPE) {
            type = PaymentProviderType.STRIPE;
        } else {
            type = PaymentProviderType.PAYSTACK;
        }
        return providers.stream()
                .filter(p -> p.providerType() == type)
                .findFirst()
                .orElse(null);
    }
}
