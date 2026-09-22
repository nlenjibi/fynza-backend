package ecommerce.modules.payment.provider.impl;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.modules.payment.config.PaymentProperties;
import ecommerce.modules.payment.exception.PaymentProviderException;
import ecommerce.modules.payment.provider.PaymentProvider;
import ecommerce.modules.payment.provider.PaymentProviderType;
import ecommerce.modules.payment.provider.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fynza.payment.provider", havingValue = "paystack", matchIfMissing = true)
public class PaystackPaymentProvider implements PaymentProvider {

    private static final String PROVIDER = "PAYSTACK";
    private static final String SIG_HEADER = "x-paystack-signature";

    private final PaymentProperties props;
    private final RestTemplate paystackRestTemplate;

    @Override
    public PaymentProviderType providerType() {
        return PaymentProviderType.PAYSTACK;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        String secret = props.getPaystack().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[Paystack] Webhook secret not configured — skipping signature check");
            return true;
        }
        String signature = headers.get(SIG_HEADER);
        if (signature == null) {
            log.warn("[Paystack] Missing {} header", SIG_HEADER);
            return false;
        }
        return hmacSha256Hex(payload, secret).equals(signature);
    }

    @Override
    public PaymentInitiateResult initiate(PaymentInitiateRequest request) {
        PaymentProperties.PaystackConfig cfg = props.getPaystack();

        String reference = (request.getReference() != null && !request.getReference().isBlank())
                ? request.getReference()
                : "FYN-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        // Paystack expects amount in smallest currency unit (pesewas for GHS, kobo for NGN)
        long amountMinorUnit = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        String body = String.format(
                "{\"email\":\"%s\",\"amount\":%d,\"reference\":\"%s\",\"currency\":\"%s\"" +
                ",\"callback_url\":\"%s\",\"metadata\":%s}",
                request.getEmail(), amountMinorUnit, reference, request.getCurrency(),
                request.getCallbackUrl() != null ? request.getCallbackUrl() : "",
                request.getMetadata() != null ? request.getMetadata() : "{}");

        try {
            JsonNode raw = post(cfg.getBaseUrl() + "/transaction/initialize", body, cfg.getSecretKey());
            JsonNode data = raw.path("data");
            String ref = data.path("reference").asText(reference);
            log.info("[Paystack] Payment initiated reference={}", ref);
            return PaymentInitiateResult.builder()
                    .reference(ref)
                    .authorizationUrl(data.path("authorization_url").asText(null))
                    .accessCode(data.path("access_code").asText(null))
                    .build();
        } catch (HttpClientErrorException e) {
            log.error("[Paystack] initiate HTTP error: {}", e.getResponseBodyAsString());
            throw new PaymentProviderException(PROVIDER, "Payment initiation failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentVerifyResult verify(PaymentVerifyRequest request) {
        PaymentProperties.PaystackConfig cfg = props.getPaystack();
        String url = cfg.getBaseUrl() + "/transaction/verify/" + request.getReference();

        try {
            JsonNode raw = get(url, cfg.getSecretKey());
            JsonNode data = raw.path("data");
            BigDecimal amount = BigDecimal.valueOf(data.path("amount").asLong(0))
                    .divide(BigDecimal.valueOf(100));
            String status = mapStatus(data.path("status").asText("unknown"));
            log.info("[Paystack] Verified reference={} status={}", sanitize(request.getReference()), status);
            return PaymentVerifyResult.builder()
                    .reference(data.path("reference").asText(request.getReference()))
                    .status(status)
                    .amount(amount)
                    .currency(data.path("currency").asText())
                    .providerTransactionId(String.valueOf(data.path("id").asLong()))
                    .channel(data.path("channel").asText(null))
                    .gatewayResponse(data.path("gateway_response").asText(null))
                    .build();
        } catch (HttpClientErrorException e) {
            log.error("[Paystack] verify HTTP error: {}", e.getResponseBodyAsString());
            throw new PaymentProviderException(PROVIDER, "Payment verification failed: " + e.getMessage(), e);
        }
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        PaymentProperties.PaystackConfig cfg = props.getPaystack();

        String body = request.getAmount() != null
                ? String.format("{\"transaction\":\"%s\",\"amount\":%d}",
                        request.getReference(),
                        request.getAmount().multiply(BigDecimal.valueOf(100)).longValue())
                : String.format("{\"transaction\":\"%s\"}", request.getReference());

        try {
            JsonNode raw = post(cfg.getBaseUrl() + "/refund", body, cfg.getSecretKey());
            JsonNode data = raw.path("data");
            log.info("[Paystack] Refund initiated reference={}", sanitize(request.getReference()));
            return RefundResult.builder()
                    .refundId(String.valueOf(data.path("id").asLong()))
                    .reference(request.getReference())
                    .amount(request.getAmount())
                    .currency(data.path("currency").asText(null))
                    .status(data.path("status").asText("pending"))
                    .build();
        } catch (HttpClientErrorException e) {
            log.error("[Paystack] refund HTTP error: {}", e.getResponseBodyAsString());
            throw new PaymentProviderException(PROVIDER, "Refund failed: " + e.getMessage(), e);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private JsonNode post(String url, String body, String secretKey) {
        JsonNode raw = paystackRestTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(secretKey)),
                JsonNode.class).getBody();
        assertSuccess(raw);
        return raw;
    }

    private JsonNode get(String url, String secretKey) {
        JsonNode raw = paystackRestTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(secretKey)),
                JsonNode.class).getBody();
        assertSuccess(raw);
        return raw;
    }

    private void assertSuccess(JsonNode raw) {
        if (raw == null || !raw.path("status").asBoolean()) {
            String msg = raw != null ? raw.path("message").asText("Unknown error") : "Null response";
            throw new PaymentProviderException(PROVIDER, msg);
        }
    }

    private HttpHeaders bearerHeaders(String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);
        return headers;
    }

    private String mapStatus(String paystackStatus) {
        return switch (paystackStatus.toLowerCase()) {
            case "success"   -> "SUCCESS";
            case "failed"    -> "FAILED";
            case "abandoned" -> "ABANDONED";
            case "reversed"  -> "REVERSED";
            default          -> paystackStatus.toUpperCase();
        };
    }

    private static String sanitize(String value) {
        if (value == null) return "";
        return value.replace('\r', '_').replace('\n', '_').replace('\t', '_');
    }

    private String hmacSha256Hex(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HMAC computation failed", e);
        }
    }
}
