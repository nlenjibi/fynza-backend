package ecommerce.modules.payout.provider;

import ecommerce.modules.payout.enums.PayoutProviderType;
import ecommerce.modules.payout.provider.dto.PayoutDispatchRequest;
import ecommerce.modules.payout.provider.dto.PayoutDispatchResult;
import ecommerce.modules.payout.provider.dto.PayoutStatusResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "fynza.payout.provider", havingValue = "stripe")
public class StripePayoutProvider implements PayoutProvider {

    private static final String PROVIDER = "STRIPE";

    @Override
    public PayoutProviderType providerType() {
        return PayoutProviderType.STRIPE;
    }

    @Override
    public PayoutDispatchResult dispatch(PayoutDispatchRequest request) {
        throw new PayoutProviderException(PROVIDER, "Stripe payout provider is not yet implemented");
    }

    @Override
    public PayoutStatusResult getStatus(String providerReference) {
        throw new PayoutProviderException(PROVIDER, "Stripe payout provider is not yet implemented");
    }

    @Override
    public boolean cancelPayout(String providerReference) {
        throw new PayoutProviderException(PROVIDER, "Stripe payout provider is not yet implemented");
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        log.warn("[Stripe] Webhook signature verification not yet implemented");
        return false;
    }
}
