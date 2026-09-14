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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "fynza.media.provider.primary", havingValue = "CLOUDINARY")
@RequiredArgsConstructor
public class CloudinaryStorageProvider implements MediaStorageProvider {

    private static final StorageCapabilities CAPABILITIES = new StorageCapabilities(
            false, false, true, false, false, true, true, true, true, false
    );
    private static final String CLOUDINARY_UPLOAD_ENDPOINT = "https://api.cloudinary.com/v1_1";

    private final MediaProperties props;
    private final ObjectMapper     objectMapper;

    private HttpClient httpClient;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.CLOUDINARY;
    }

    @Override
    public StorageCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public UploadSession createUploadSession(UploadRequest request) {
        MediaProperties.CloudinaryConfig cfg = props.getProvider().getCloudinary();
        String publicId = buildPublicId(request);
        long timestamp = Instant.now().getEpochSecond();

        TreeMap<String, String> params = new TreeMap<>();
        params.put("public_id", publicId);
        params.put("timestamp", String.valueOf(timestamp));
        if (cfg.getUploadPreset() != null && !cfg.getUploadPreset().isBlank()) {
            params.put("upload_preset", cfg.getUploadPreset());
        }

        String signature = signParams(params, cfg.getApiSecret());
        StringBuilder uploadUrl = new StringBuilder(CLOUDINARY_UPLOAD_ENDPOINT)
                .append("/").append(cfg.getCloudName()).append("/image/upload")
                .append("?api_key=").append(cfg.getApiKey())
                .append("&timestamp=").append(timestamp)
                .append("&public_id=").append(publicId)
                .append("&signature=").append(signature);
        if (cfg.getUploadPreset() != null && !cfg.getUploadPreset().isBlank()) {
            uploadUrl.append("&upload_preset=").append(cfg.getUploadPreset());
        }

        return new UploadSession(
                UUID.randomUUID(),
                ProviderType.CLOUDINARY,
                UploadMethod.SIGNED_UPLOAD,
                publicId,
                uploadUrl.toString(),
                Map.of(),
                Instant.now().plusSeconds(cfg.getSignedUrlExpirySeconds())
        );
    }

    @Override
    public UploadResult verifyUpload(UploadSession session) {
        MediaProperties.CloudinaryConfig cfg = props.getProvider().getCloudinary();
        String url = CLOUDINARY_UPLOAD_ENDPOINT + "/" + cfg.getCloudName()
                + "/resources/image/upload/" + session.objectKey();

        try {
            String credentials = cfg.getApiKey() + ":" + cfg.getApiSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);

            long fileSize = body.get("bytes") instanceof Number n ? n.longValue() : 0L;
            String etag = (String) body.get("etag");
            String cdnUrl = cfg.getCdnBaseUrl() != null
                    ? cfg.getCdnBaseUrl() + "/" + session.objectKey()
                    : (String) body.get("secure_url");

            return new UploadResult(session.objectKey(), etag, null, fileSize, cdnUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Cloudinary upload", e);
        }
    }

    @Override
    public void deleteObject(StorageObject object) {
        MediaProperties.CloudinaryConfig cfg = props.getProvider().getCloudinary();
        long timestamp = Instant.now().getEpochSecond();

        TreeMap<String, String> params = new TreeMap<>();
        params.put("public_id", object.objectKey());
        params.put("timestamp", String.valueOf(timestamp));
        String signature = signParams(params, cfg.getApiSecret());

        String formBody = "public_id=" + object.objectKey()
                + "&timestamp=" + timestamp
                + "&api_key=" + cfg.getApiKey()
                + "&signature=" + signature;

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(CLOUDINARY_UPLOAD_ENDPOINT + "/" + cfg.getCloudName() + "/image/destroy"))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .build();

            httpClient().send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete Cloudinary object", e);
        }
    }

    @Override
    public StorageObjectMetadata getMetadata(StorageObject object) {
        MediaProperties.CloudinaryConfig cfg = props.getProvider().getCloudinary();
        String url = CLOUDINARY_UPLOAD_ENDPOINT + "/" + cfg.getCloudName()
                + "/resources/image/upload/" + object.objectKey();

        try {
            String credentials = cfg.getApiKey() + ":" + cfg.getApiSecret();
            String basicAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Basic " + basicAuth)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);

            long fileSize = body.get("bytes") instanceof Number n ? n.longValue() : 0L;
            String format = (String) body.get("format");
            return new StorageObjectMetadata(object.objectKey(), fileSize, "image/" + format, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Cloudinary metadata", e);
        }
    }

    @Override
    public String createDownloadUrl(StorageObject object, Duration expiration) {
        MediaProperties.CloudinaryConfig cfg = props.getProvider().getCloudinary();
        long timestamp = Instant.now().plusSeconds(expiration.getSeconds()).getEpochSecond();

        // Signed URL: https://res.cloudinary.com/{cloud}/image/upload/s--{sig}--/{publicId}
        String toSign = object.objectKey() + cfg.getApiSecret() + timestamp;
        String sig;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(toSign.getBytes(StandardCharsets.UTF_8));
            sig = bytesToHex(hash).substring(0, 8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to sign Cloudinary URL", e);
        }

        return "https://res.cloudinary.com/" + cfg.getCloudName()
                + "/image/upload/s--" + sig + "--/" + object.objectKey();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String signParams(TreeMap<String, String> params, String apiSecret) {
        StringBuilder sb = new StringBuilder();
        params.forEach((k, v) -> {
            if (!sb.isEmpty()) sb.append("&");
            sb.append(k).append("=").append(v);
        });
        sb.append(apiSecret);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute Cloudinary signature", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) hex.append(String.format("%02x", b));
        return hex.toString();
    }

    private HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();
        }
        return httpClient;
    }

    private String buildPublicId(UploadRequest request) {
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
