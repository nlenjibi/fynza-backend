package ecommerce.modules.payout.provider;

import com.fasterxml.jackson.databind.JsonNode;
import ecommerce.modules.payout.enums.PayoutProviderType;
import ecommerce.modules.payout.provider.dto.PayoutDispatchRequest;
import ecommerce.modules.payout.provider.dto.PayoutDispatchResult;
import ecommerce.modules.payout.provider.dto.PayoutStatusResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fynza.payout.provider", havingValue = "paystack", matchIfMissing = true)
public class PaystackPayoutProvider implements PayoutProvider {

    private static final String PROVIDER = "PAYSTACK";
    private static final String SIG_HEADER = "x-paystack-signature";

    private final PayoutProperties props;

    @Qualifier("paystackPayoutRestTemplate")
    private final RestTemplate restTemplate;

    @Override
    public PayoutProviderType providerType() {
        return PayoutProviderType.PAYSTACK;
    }

    @Override
    public PayoutDispatchResult dispatch(PayoutDispatchRequest request) {
        PayoutProperties.PaystackPayoutConfig cfg = props.getPaystack();
        String baseUrl = cfg.getBaseUrl();

        try {
            // Step 1: Create transfer recipient
            String recipientBody = String.format(
                    "{\"type\":\"mobile_money\",\"name\":\"%s\",\"account_number\":\"%s\",\"bank_code\":\"%s\",\"currency\":\"%s\"}",
                    request.getAccountName(),
                    request.getAccountIdentifier(),
                    request.getBankCode() != null ? request.getBankCode() : "",
                    request.getCurrency());

            JsonNode recipientResponse = post(baseUrl + "/transferrecipient", recipientBody, cfg.getSecretKey());
            String recipientCode = recipientResponse.path("data").path("recipient_code").asText();

            log.info("[Paystack] Transfer recipient created recipientCode={} payoutId={}",
                    sanitize(recipientCode), sanitize(request.getPayoutPublicId().toString()));

            // Step 2: Initiate transfer
            long amountMinorUnit = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();
            String transferBody = String.format(
                    "{\"source\":\"balance\",\"amount\":%d,\"recipient\":\"%s\",\"reason\":\"%s\",\"reference\":\"%s\"}",
                    amountMinorUnit,
                    recipientCode,
                    request.getNarration() != null ? request.getNarration() : "Payout",
                    request.getIdempotencyKey());

            JsonNode transferResponse = post(baseUrl + "/transfer", transferBody, cfg.getSecretKey());
            String transferCode = transferResponse.path("data").path("transfer_code").asText();

            log.info("[Paystack] Transfer initiated transferCode={} reference={}",
                    sanitize(transferCode), sanitize(request.getIdempotencyKey()));

            return PayoutDispatchResult.builder()
                    .success(true)
                    .providerReference(transferCode)
                    .status("PENDING")
                    .message("Transfer initiated successfully")
                    .build();

        } catch (HttpClientErrorException e) {
            log.error("[Paystack] dispatch HTTP error: {}", e.getResponseBodyAsString());
            throw new PayoutProviderException(PROVIDER, "Payout dispatch failed: " + e.getMessage(), e);
        }
    }

    @Override
    public PayoutStatusResult getStatus(String providerReference) {
        PayoutProperties.PaystackPayoutConfig cfg = props.getPaystack();
        String url = cfg.getBaseUrl() + "/transfer/" + providerReference;

        try {
            JsonNode raw = get(url, cfg.getSecretKey());
            JsonNode data = raw.path("data");
            String status = mapTransferStatus(data.path("status").asText("unknown"));
            String failureReason = data.path("failure_reason").asText(null);

            log.info("[Paystack] Transfer status providerRef={} status={}",
                    sanitize(providerReference), sanitize(status));

            return PayoutStatusResult.builder()
                    .providerReference(providerReference)
                    .status(status)
                    .failureReason(failureReason)
                    .updatedAt(Instant.now())
                    .build();
        } catch (HttpClientErrorException e) {
            log.error("[Paystack] getStatus HTTP error for providerRef={}: {}",
                    sanitize(providerReference), e.getResponseBodyAsString());
            throw new PayoutProviderException(PROVIDER, "Status check failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean cancelPayout(String providerReference) {
        log.warn("[Paystack] Direct payout cancellation via API is not supported. providerRef={}",
                sanitize(providerReference));
        return false;
    }

    @Override
    public boolean verifyWebhookSignature(String payload, Map<String, String> headers) {
        String secret = props.getPaystack().getWebhookSecret();
        if (secret == null || secret.isBlank()) {
            log.warn("[Paystack] Payout webhook secret not configured — skipping signature check");
            return true;
        }
        String signature = headers.get(SIG_HEADER);
        if (signature == null) {
            log.warn("[Paystack] Missing {} header in payout webhook", SIG_HEADER);
            return false;
        }
        return hmacSha256Hex(payload, secret).equals(signature);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private JsonNode post(String url, String body, String secretKey) {
        JsonNode raw = restTemplate.exchange(
                url, HttpMethod.POST,
                new HttpEntity<>(body, bearerHeaders(secretKey)),
                JsonNode.class).getBody();
        assertSuccess(raw);
        return raw;
    }

    private JsonNode get(String url, String secretKey) {
        JsonNode raw = restTemplate.exchange(
                url, HttpMethod.GET,
                new HttpEntity<>(bearerHeaders(secretKey)),
                JsonNode.class).getBody();
        assertSuccess(raw);
        return raw;
    }

    private void assertSuccess(JsonNode raw) {
        if (raw == null || !raw.path("status").asBoolean()) {
            String msg = raw != null ? raw.path("message").asText("Unknown error") : "Null response";
            throw new PayoutProviderException(PROVIDER, msg);
        }
    }

    private HttpHeaders bearerHeaders(String secretKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + secretKey);
        return headers;
    }

    private String mapTransferStatus(String paystackStatus) {
        return switch (paystackStatus.toLowerCase()) {
            case "success"    -> "COMPLETED";
            case "failed"     -> "FAILED";
            case "reversed"   -> "REVERSED";
            case "pending"    -> "PROCESSING";
            case "otp"        -> "PROCESSING";
            default           -> paystackStatus.toUpperCase();
        };
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

    private static String sanitize(String value) {
        return value == null ? "" : value.replaceAll("[\\r\\n\\t]", "_");
    }
}
