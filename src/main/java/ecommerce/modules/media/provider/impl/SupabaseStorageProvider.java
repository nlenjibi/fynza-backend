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
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "fynza.media.provider.primary", havingValue = "SUPABASE")
@RequiredArgsConstructor
public class SupabaseStorageProvider implements MediaStorageProvider {

    private static final StorageCapabilities CAPABILITIES = new StorageCapabilities(
            false, false, true, false, false, true, true, false, true, false
    );

    private final MediaProperties props;
    private final ObjectMapper     objectMapper;

    private HttpClient httpClient;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.SUPABASE;
    }

    @Override
    public StorageCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public UploadSession createUploadSession(UploadRequest request) {
        MediaProperties.SupabaseConfig cfg = props.getProvider().getSupabase();
        String objectKey = buildObjectKey(request);

        try {
            String url = cfg.getUrl() + "/storage/v1/object/upload/sign/" + cfg.getBucket() + "/" + objectKey;
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + cfg.getServiceRoleKey())
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient().send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
            String signedUrl = (String) body.get("url");

            return new UploadSession(
                    UUID.randomUUID(),
                    ProviderType.SUPABASE,
                    UploadMethod.SIGNED_UPLOAD,
                    objectKey,
                    signedUrl,
                    Map.of("Content-Type", request.mimeType()),
                    Instant.now().plusSeconds(cfg.getSignedUrlExpirySeconds())
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Supabase upload session", e);
        }
    }

    @Override
    public UploadResult verifyUpload(UploadSession session) {
        MediaProperties.SupabaseConfig cfg = props.getProvider().getSupabase();
        String url = cfg.getUrl() + "/storage/v1/object/" + cfg.getBucket() + "/" + session.objectKey();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + cfg.getServiceRoleKey())
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient().send(request, HttpResponse.BodyHandlers.discarding());
            long fileSize = response.headers().firstValueAsLong("content-length").orElse(0L);
            String etag = response.headers().firstValue("etag").orElse(null);

            String cdnUrl = cfg.getCdnBaseUrl() != null
                    ? cfg.getCdnBaseUrl() + "/" + session.objectKey()
                    : cfg.getUrl() + "/storage/v1/object/public/" + cfg.getBucket() + "/" + session.objectKey();

            return new UploadResult(session.objectKey(), etag, null, fileSize, cdnUrl);
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify Supabase upload", e);
        }
    }

    @Override
    public void deleteObject(StorageObject object) {
        MediaProperties.SupabaseConfig cfg = props.getProvider().getSupabase();
        String url = cfg.getUrl() + "/storage/v1/object/" + cfg.getBucket();
        String bodyJson = "{\"prefixes\":[\"" + object.objectKey() + "\"]}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + cfg.getServiceRoleKey())
                    .header("Content-Type", "application/json")
                    .DELETE()
                    .build();

            // Supabase bulk delete requires a body with DELETE; use POST workaround
            HttpRequest deleteRequest = HttpRequest.newBuilder()
                    .uri(URI.create(cfg.getUrl() + "/storage/v1/object/" + cfg.getBucket()))
                    .header("Authorization", "Bearer " + cfg.getServiceRoleKey())
                    .header("Content-Type", "application/json")
                    .method("DELETE", HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            httpClient().send(deleteRequest, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete Supabase object", e);
        }
    }

    @Override
    public StorageObjectMetadata getMetadata(StorageObject object) {
        MediaProperties.SupabaseConfig cfg = props.getProvider().getSupabase();
        String url = cfg.getUrl() + "/storage/v1/object/" + cfg.getBucket() + "/" + object.objectKey();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + cfg.getServiceRoleKey())
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<Void> response = httpClient().send(request, HttpResponse.BodyHandlers.discarding());
            long size = response.headers().firstValueAsLong("content-length").orElse(0L);
            String contentType = response.headers().firstValue("content-type").orElse(null);
            String etag = response.headers().firstValue("etag").orElse(null);

            return new StorageObjectMetadata(object.objectKey(), size, contentType, etag, null);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get Supabase object metadata", e);
        }
    }

    @Override
    public String createDownloadUrl(StorageObject object, Duration expiration) {
        MediaProperties.SupabaseConfig cfg = props.getProvider().getSupabase();
        String url = cfg.getUrl() + "/storage/v1/object/sign/" + cfg.getBucket() + "/" + object.objectKey();
        String bodyJson = "{\"expiresIn\":" + expiration.getSeconds() + "}";

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + cfg.getServiceRoleKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                    .build();

            HttpResponse<String> response = httpClient().send(request, HttpResponse.BodyHandlers.ofString());
            Map<?, ?> body = objectMapper.readValue(response.body(), Map.class);
            String signedUrl = (String) body.get("signedURL");

            return cfg.getUrl() + signedUrl;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Supabase signed download URL", e);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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
