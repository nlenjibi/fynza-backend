package ecommerce.modules.media.provider.impl;

import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadMethod;
import ecommerce.modules.media.provider.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "fynza.media.provider.primary", havingValue = "LOCAL")
@RequiredArgsConstructor
public class LocalStorageProvider implements MediaStorageProvider {

    private static final StorageCapabilities CAPABILITIES = new StorageCapabilities(
            false, false, false, false, false, false, true, false, false, false
    );

    private final MediaProperties props;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.LOCAL;
    }

    @Override
    public StorageCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public UploadSession createUploadSession(UploadRequest request) {
        MediaProperties.LocalConfig cfg = props.getProvider().getLocal();
        String objectKey = buildObjectKey(request);
        String encodedKey = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(objectKey.getBytes(StandardCharsets.UTF_8));
        String uploadUrl = cfg.getUploadApiUrl() + "/v1/media/local/upload?k=" + encodedKey;

        return new UploadSession(
                UUID.randomUUID(),
                ProviderType.LOCAL,
                UploadMethod.SERVER_PROXY,
                objectKey,
                uploadUrl,
                Map.of("Content-Type", request.mimeType()),
                Instant.now().plusSeconds(900)
        );
    }

    @Override
    public UploadResult verifyUpload(UploadSession session) {
        MediaProperties.LocalConfig cfg = props.getProvider().getLocal();
        Path filePath = resolveStoragePath(session.objectKey());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("Local file not found after upload: " + session.objectKey());
        }

        try {
            long fileSize = Files.size(filePath);
            String cdnUrl = cfg.getBaseUrl() + "/" + session.objectKey();
            return new UploadResult(session.objectKey(), null, null, fileSize, cdnUrl);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read local file metadata", e);
        }
    }

    @Override
    public void deleteObject(StorageObject object) {
        Path filePath = resolveStoragePath(object.objectKey());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete local file: " + object.objectKey(), e);
        }
    }

    @Override
    public StorageObjectMetadata getMetadata(StorageObject object) {
        Path filePath = resolveStoragePath(object.objectKey());
        try {
            long size = Files.size(filePath);
            String contentType = Files.probeContentType(filePath);
            Instant lastModified = Files.getLastModifiedTime(filePath).toInstant();
            return new StorageObjectMetadata(object.objectKey(), size, contentType, null, lastModified);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read local file metadata: " + object.objectKey(), e);
        }
    }

    @Override
    public String createDownloadUrl(StorageObject object, Duration expiration) {
        MediaProperties.LocalConfig cfg = props.getProvider().getLocal();
        return cfg.getBaseUrl() + "/" + object.objectKey();
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Path resolveStoragePath(String objectKey) {
        MediaProperties.LocalConfig cfg = props.getProvider().getLocal();
        return Paths.get(cfg.getStorageDir()).resolve(objectKey).normalize();
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
