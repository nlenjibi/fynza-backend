package ecommerce.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final String KEY_PREFIX = "idempotency:";

    public Optional<String> check(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }

        String key = KEY_PREFIX + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(key);

        if (cached != null && cached.contains("|")) {
            return Optional.of(cached.split("\\|")[1]);
        }

        return Optional.empty();
    }

    public String save(String idempotencyKey, Object requestPayload, Object response) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }

        String key = KEY_PREFIX + idempotencyKey;
        
        try {
            String payloadHash = computePayloadHash(requestPayload);
            String responseJson = objectMapper.writeValueAsString(response);
            
            String combined = payloadHash + "|" + responseJson;
            
            redisTemplate.opsForValue().set(key, combined, DEFAULT_TTL);
            
            return responseJson;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to process idempotency record", e);
        }
    }

    public boolean validatePayload(String idempotencyKey, Object requestPayload) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return true;
        }

        String key = KEY_PREFIX + idempotencyKey;
        String cached = redisTemplate.opsForValue().get(key);

        if (cached == null) {
            return true;
        }

        String storedHash = cached.split("\\|")[0];
        String incomingHash = computePayloadHash(requestPayload);

        return storedHash.equals(incomingHash);
    }

    private String computePayloadHash(Object payload) {
        if (payload == null) {
            return "null";
        }

        try {
            String json = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            return String.valueOf(payload.hashCode());
        }
    }

    public void delete(String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String key = KEY_PREFIX + idempotencyKey;
            redisTemplate.delete(key);
        }
    }
}