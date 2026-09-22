package ecommerce.modules.payout.provider;

import ecommerce.modules.payout.enums.PayoutProviderType;
import ecommerce.modules.payout.provider.dto.PayoutDispatchRequest;
import ecommerce.modules.payout.provider.dto.PayoutDispatchResult;
import ecommerce.modules.payout.provider.dto.PayoutStatusResult;

import java.util.Map;

public interface PayoutProvider {

    PayoutProviderType providerType();

    PayoutDispatchResult dispatch(PayoutDispatchRequest request);

    PayoutStatusResult getStatus(String providerReference);

    boolean cancelPayout(String providerReference);

    boolean verifyWebhookSignature(String payload, Map<String, String> headers);
}
