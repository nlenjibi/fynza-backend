package ecommerce.modules.payment.provider;

import ecommerce.modules.payment.provider.dto.*;

import java.util.Map;

public interface PaymentProvider {

    PaymentProviderType providerType();

    boolean verifyWebhookSignature(String payload, Map<String, String> headers);

    PaymentInitiateResult initiate(PaymentInitiateRequest request);

    PaymentVerifyResult verify(PaymentVerifyRequest request);

    RefundResult refund(RefundRequest request);
}
