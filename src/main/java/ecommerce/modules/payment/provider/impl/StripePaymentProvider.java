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
@ConditionalOnProperty(name = "fynza.payment.provider", havingValue = "stripe")
public class StripePaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "STRIPE";
    private static final String SIG_HEADER = "stripe-signature";

    private final PaymentProperties props;

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.STRIPE;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        String secret = props.getStripe().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[Stripe] Webhook secret not configured — skipping signature check");
            return true;
        }
        // Stripe uses a "t=timestamp,v1=signature" format verified via stripe-java SDK.
        // Full implementation requires the Stripe SDK dependency.
        String signature = headers.get(SIG_HEADER);
        if (signature == null) {
            log.warn("[Stripe] Missing {} header", SIG_HEADER);
            return false;
        }
        throw new PaymentProviderException(PROVIDER, "Stripe webhook verification not yet implemented — add stripe-java SDK");
    }

    @Override
    public PaymentInitiateResult initiate(PaymentInitiateRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "Stripe payment initiation not yet implemented — add stripe-java SDK and configure fynza.payment.stripe.*");
    }

    @Override
    public PaymentVerifyResult verify(PaymentVerifyRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "Stripe payment verification not yet implemented — add stripe-java SDK and configure fynza.payment.stripe.*");
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "Stripe refund not yet implemented — add stripe-java SDK and configure fynza.payment.stripe.*");
    }
}
