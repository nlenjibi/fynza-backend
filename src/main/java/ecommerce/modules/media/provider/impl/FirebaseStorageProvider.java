package ecommerce.modules.media.provider.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadMethod;
import ecommerce.modules.media.provider.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Component
@ConditionalOnProperty(name = "fynza.media.provider.primary", havingValue = "FIREBASE")
@RequiredArgsConstructor
public class FirebaseStorageProvider implements MediaStorageProvider {

    private static final StorageCapabilities CAPABILITIES = new StorageCapabilities(
            false, false, false, true, false, true, true, false, false, false
    );
    private static final String GCS_BASE = "https://storage.googleapis.com";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String SCOPE     = "https://www.googleapis.com/auth/devstorage.read_write";

    private final MediaProperties props;
    private final ObjectMapper     objectMapper;

    private HttpClient httpClient;

    private final AtomicReference<String>  cachedToken      = new AtomicReference<>();
    private volatile Instant               tokenExpiresAt   = Instant.EPOCH;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.FIREBASE;
    }

    @Override
    public StorageCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public UploadSession createUploadSession(UploadRequest request) {
        MediaProperties.FirebaseConfig cfg = props.getProvider().getFirebase();
        String objectKey = buildObjectKey(request);
        String token = accessToken();
        String encodedKey = URLEncoder.encode(objectKey, StandardCharsets.UTF_8);

        String initiateUrl = GCS_BASE + "/upload/storage/v1/b/" + cfg.getBucket()
                + "/o?uploadType=resumable&name=" + encodedKey;
        try {
            HttpRequest httpReq = HttpRequest.newBuilder()
                    .uri(URI.create(initiateUrl))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .header("X-Upload-Content-Type", request.mimeType())
                    .header("X-Upload-Content-Length", String.valueOf(request.fileSize()))
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient().send(httpReq, HttpResponse.BodyHandlers.ofString());
            String resumableUrl = response.headers().firstValue("location")
                    .orElseThrow(() -> new RuntimeException("No Location header in Firebase resumable upload response"));

            return new UploadSession(
                    UUID.randomUUID(),
                    ProviderType.FIREBASE,
                    UploadMethod.RESUMABLE,
                    objectKey,
                    resumableUrl,
                    Map.of("Content-Type", request.mimeType()),
                    Instant.now().plusSeconds(600)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initiate Firebase resumable upload", e);
        }
    }

    @Override
    public UploadResult verifyUpload(UploadSession session) {
        MediaProperties.FirebaseConfig cfg = props.getProvider().getFirebase();
        String encodedKey = URLEncoder.encode(session.objectKey(), StandardCharsets.UTF_8);
        String metaUrl = GCS_BASE + "/storage/v1/b/" + cfg.getBucket() + "/o/" + encodedKey;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(metaUrl))
                    .header("Authorization", "Bearer " + accessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
            Object sizeObj = body.get("size");
            long fileSize = sizeObj != null ? Long.parseLong((String) sizeObj) : 0L;
            String etag = (String) body.get("etag");

            String cdnUrl = cfg.getCdnBaseUrl() != null
                    ? cfg.getCdnBaseUrl() + "/" + session.objectKey()
                    : "https://firebasestorage.googleapis.com/v0/b/" + cfg.getBucket()
                        + "/o/" + encodedKey + "?alt=media";

            return new UploadResult(session.objectKey(), etag, null, fileSize, cdnUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Firebase upload", e);
        }
    }

    @Override
    public void deleteObject(StorageObject object) {
        MediaProperties.FirebaseConfig cfg = props.getProvider().getFirebase();
        String encodedKey = URLEncoder.encode(object.objectKey(), StandardCharsets.UTF_8);
        String deleteUrl = GCS_BASE + "/storage/v1/b/" + cfg.getBucket() + "/o/" + encodedKey;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(deleteUrl))
                    .header("Authorization", "Bearer " + accessToken())
                    .DELETE()
                    .build();

            httpClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete Firebase object", e);
        }
    }

    @Override
    public StorageObjectMetadata getMetadata(StorageObject object) {
        MediaProperties.FirebaseConfig cfg = props.getProvider().getFirebase();
        String encodedKey = URLEncoder.encode(object.objectKey(), StandardCharsets.UTF_8);
        String metaUrl = GCS_BASE + "/storage/v1/b/" + cfg.getBucket() + "/o/" + encodedKey;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(metaUrl))
                    .header("Authorization", "Bearer " + accessToken())
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
            Object sizeVal = body.get("size");
            long size = sizeVal != null ? Long.parseLong((String) sizeVal) : 0L;
            String contentType = (String) body.get("contentType");
            String etag = (String) body.get("etag");

            return new StorageObjectMetadata(object.objectKey(), size, contentType, etag, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Firebase metadata", e);
        }
    }

    @Override
    public String createDownloadUrl(StorageObject object, Duration expiration) {
        MediaProperties.FirebaseConfig cfg = props.getProvider().getFirebase();
        String encodedKey = URLEncoder.encode(object.objectKey(), StandardCharsets.UTF_8);
        // Returns a time-limited URL via access_token (valid for ~1 hour, same as the token TTL)
        return GCS_BASE + "/download/storage/v1/b/" + cfg.getBucket()
                + "/o/" + encodedKey + "?alt=media&access_token=" + accessToken();
    }

    // ── OAuth2 token management ────────────────────────────────────────────────

    private synchronized String accessToken() {
        if (cachedToken.get() != null && Instant.now().isBefore(tokenExpiresAt.minusSeconds(60))) {
            return cachedToken.get();
        }

        MediaProperties.FirebaseConfig cfg = props.getProvider().getFirebase();
        try {
            Map<?, ?> sa = objectMapper.readValue(cfg.getServiceAccountJson(), Map.class);
            String clientEmail = (String) sa.get("client_email");
            String privateKeyPem = (String) sa.get("private_key");

            PrivateKey privateKey = parsePemPrivateKey(privateKeyPem);
            String jwt = buildJwt(clientEmail, privateKey);

            String body = "grant_type=urn%3Aietf%3Aparams%3Aoauth2%3Agrant-type%3Ajwt-bearer&assertion=" + jwt;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> tokenBody = objectMapper.readValue(response.body(), Map.class);
            String token = (String) tokenBody.get("access_token");
            Object expObj = tokenBody.get("expires_in");
            int expiresIn = expObj instanceof Number n ? n.intValue() : 3600;

            cachedToken.set(token);
            tokenExpiresAt = Instant.now().plusSeconds(expiresIn);
            return token;
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain Firebase access token", e);
        }
    }

    private String buildJwt(String clientEmail, PrivateKey privateKey) throws Exception {
        long now = Instant.now().getEpochSecond();
        String header  = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(("{\"iss\":\"" + clientEmail + "\","
                        + "\"scope\":\"" + SCOPE + "\","
                        + "\"aud\":\"" + TOKEN_URL + "\","
                        + "\"iat\":" + now + ","
                        + "\"exp\":" + (now + 3600) + "}").getBytes(StandardCharsets.UTF_8));

        String signingInput = header + "." + payload;
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(signingInput.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());

        return signingInput + "." + signature;
    }

    private PrivateKey parsePemPrivateKey(String pem) throws Exception {
        String cleaned = pem
                .replace("-----BEGIN RSA PRIVATE KEY-----", "")
                .replace("-----END RSA PRIVATE KEY-----", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(cleaned);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        return KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

    // ── misc helpers ──────────────────────────────────────────────────────────

    private HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
        return httpClient;
    }

    private String buildObjectKey(UploadRequest request) {
        String ext = extractExtension(request.filename());
        return String.format("%s/%s/%s/original%s",
                request.ownerType().name().toLowerCase(),
                request.ownerId(),
                UUID.randomUUID(),
                ext.isBlank() ? "" : "." + ext
        );
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
