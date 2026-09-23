package ecommerce.modules.payment.service;

import ecommerce.modules.payment.provider.PaymentProvider;
import ecommerce.modules.payment.provider.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Thin service facade over the active {@link PaymentProvider}.
 * Controllers should depend on this class; provider-specific logic lives in the provider layer.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaystackPaymentService {

    private final PaymentProvider paymentProvider;

    public PaymentInitiateResult initializePayment(PaymentInitiateRequest request) {
        log.info("Initiating payment via {} for amount {} {}",
                paymentProvider.providerType(), request.getAmount(), sanitize(request.getCurrency()));
        return paymentProvider.initiate(request);
    }

    public PaymentVerifyResult verifyPayment(String reference) {
        log.info("Verifying payment reference={} via {}", sanitize(reference), paymentProvider.providerType());
        return paymentProvider.verify(PaymentVerifyRequest.builder().reference(reference).build());
    }

    public RefundResult processRefund(String reference, BigDecimal amount) {
        log.info("Processing refund reference={} amount={} via {}", sanitize(reference), amount, paymentProvider.providerType());
        return paymentProvider.refund(RefundRequest.builder()
                .reference(reference)
                .amount(amount)
                .build());
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace('\r', '_').replace('\n', '_').replace('\t', '_');
    }
}
