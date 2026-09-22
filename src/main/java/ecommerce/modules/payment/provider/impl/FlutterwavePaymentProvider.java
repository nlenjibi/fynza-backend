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
@ConditionalOnProperty(name = "fynza.payment.provider", havingValue = "flutterwave")
public class FlutterwavePaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "FLUTTERWAVE";
    private static final String SIG_HEADER = "verif-hash";

    private final PaymentProperties props;

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.FLUTTERWAVE;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        String secret = props.getFlutterwave().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[Flutterwave] Webhook secret not configured — skipping signature check");
            return true;
        }
        // Flutterwave sends the secret hash directly in the verif-hash header.
        String hash = headers.get(SIG_HEADER);
        if (hash == null) {
            log.warn("[Flutterwave] Missing {} header", SIG_HEADER);
            return false;
        }
        return secret.equals(hash);
    }

    @Override
    public PaymentInitiateResult initiate(PaymentInitiateRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "Flutterwave payment initiation not yet implemented — configure fynza.payment.flutterwave.*");
    }

    @Override
    public PaymentVerifyResult verify(PaymentVerifyRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "Flutterwave payment verification not yet implemented — configure fynza.payment.flutterwave.*");
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new PaymentProviderException(PROVIDER,
                "Flutterwave refund not yet implemented — configure fynza.payment.flutterwave.*");
    }
}
