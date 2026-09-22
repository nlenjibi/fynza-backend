package ecommerce.modules.payment.provider.impl;

import ecommerce.modules.payment.config.PaymentProperties;
import ecommerce.modules.payment.exception.PaymentProviderException;
import ecommerce.modules.payment.provider.PaymentProvider;
import ecommerce.modules.payment.provider.PaymentProviderType;
import ecommerce.modules.payment.provider.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fynza.payment.provider", havingValue = "paypal")
public class PayPalPaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "PAYPAL";

    private final PaymentProperties props;

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.PAYPAL;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        String webhookId = props.getPaypal().getWebhookId();
        if (webhookId == null || webhookId.isBlank()) {
            log.warn("[PayPal] Webhook ID not configured — skipping signature check");
            return true;
        }
        // PayPal webhook verification requires a POST back to PayPal's /v1/notifications/verify-webhook-signature.
        // Full implementation requires OAuth2 token exchange.
        throw new PaymentProviderException(PROVIDER, "PayPal webhook verification not yet implemented — configure fynza.payment.paypal.*");
    }

    @Override
    public PaymentInitiateResult initiate(PaymentInitiateRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "PayPal payment initiation not yet implemented — configure fynza.payment.paypal.*");
    }

    @Override
    public PaymentVerifyResult verify(PaymentVerifyRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "PayPal payment verification not yet implemented — configure fynza.payment.paypal.*");
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "PayPal refund not yet implemented — configure fynza.payment.paypal.*");
    }
}
