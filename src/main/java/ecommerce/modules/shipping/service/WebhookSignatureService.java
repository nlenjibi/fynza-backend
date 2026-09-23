package ecommerce.modules.shipping.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;

@Service
@Slf4j
public class WebhookSignatureService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final Map<String, String> providerSecrets;

    public WebhookSignatureService(
            @Value("${shipping.webhook.secrets.mock:}") String mockSecret,
            @Value("${shipping.webhook.secrets.dhl:}") String dhlSecret,
            @Value("${shipping.webhook.secrets.fedex:}") String fedexSecret) {
        this.providerSecrets = Map.of(
                "mock",  mockSecret,
                "dhl",   dhlSecret,
                "fedex", fedexSecret
        );
    }

    /**
     * Verifies the HMAC-SHA256 signature of an inbound carrier webhook.
     * If no secret is configured for the provider, verification is skipped (returns true).
     * Signature header value must be "sha256=<hex-digest>".
     */
    public boolean verify(String provider, String rawPayload, String signatureHeader) {
        String secret = providerSecrets.getOrDefault(provider.toLowerCase(), "");
        if (secret == null || secret.isBlank()) {
            log.debug("No webhook secret configured for provider={}, skipping signature check", sanitize(provider));
            return true;
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Missing X-Signature header for provider={}", sanitize(provider));
            return false;
        }

        String expectedPrefix = "sha256=";
        String hexSig = signatureHeader.startsWith(expectedPrefix)
                ? signatureHeader.substring(expectedPrefix.length())
                : signatureHeader;

        try {
            String computed = computeHmac(secret, rawPayload);
            boolean valid = MessageDigest.isEqual(
                    hexSig.getBytes(StandardCharsets.UTF_8),
                    computed.getBytes(StandardCharsets.UTF_8));
            if (!valid) {
                log.warn("Invalid webhook signature for provider={}", sanitize(provider));
            }
            return valid;
        } catch (Exception e) {
            log.error("Webhook signature verification error for provider={}: {}", sanitize(provider), e.getMessage());
            return false;
        }
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.replace('\r', '_').replace('\n', '_');
    }

    private String computeHmac(String secret, String payload) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance(HMAC_SHA256);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
