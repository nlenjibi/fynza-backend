package ecommerce.modules.media.provider.impl;

import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadMethod;
import ecommerce.modules.media.provider.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class R2StorageProvider implements MediaStorageProvider {

    private static final StorageCapabilities CAPABILITIES = new StorageCapabilities(
            true, true, false, false, false, true, true, false, true, true
    );

    private final MediaProperties props;

    private S3Client    client;
    private S3Presigner presigner;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.R2;
    }

    @Override
    public StorageCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public UploadSession createUploadSession(UploadRequest request) {
        MediaProperties.R2Config r2 = props.getProvider().getR2();
        String objectKey = buildObjectKey(request);
        String uploadUrl = presigner().presignPutObject(
                PutObjectPresignRequest.builder()
                        .putObjectRequest(b -> b
                                .bucket(r2.getBucket())
                                .key(objectKey)
                                .contentType(request.mimeType())
                                .contentLength(request.fileSize())
                                .build())
                        .signatureDuration(Duration.ofSeconds(r2.getPresignedUrlExpirySeconds()))
                        .build()
        ).url().toString();

        return new UploadSession(
                UUID.randomUUID(),
                ProviderType.R2,
                UploadMethod.PRESIGNED_PUT,
                objectKey,
                uploadUrl,
                Map.of("Content-Type", request.mimeType()),
                Instant.now().plusSeconds(r2.getPresignedUrlExpirySeconds())
        );
    }

    @Override
    public UploadResult verifyUpload(UploadSession session) {
        MediaProperties.R2Config r2 = props.getProvider().getR2();
        HeadObjectResponse head = client().headObject(
                HeadObjectRequest.builder()
                        .bucket(r2.getBucket())
                        .key(session.objectKey())
                        .build()
        );
        String cdnUrl = r2.getCdnBaseUrl() != null
                ? r2.getCdnBaseUrl() + "/" + session.objectKey()
                : null;
        return new UploadResult(
                session.objectKey(),
                head.eTag(),
                head.checksumSHA256(),
                head.contentLength(),
                cdnUrl
        );
    }

    @Override
    public void deleteObject(StorageObject object) {
        client().deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(object.bucket())
                        .key(object.objectKey())
                        .build()
        );
    }

    @Override
    public StorageObjectMetadata getMetadata(StorageObject object) {
        HeadObjectResponse head = client().headObject(
                HeadObjectRequest.builder()
                        .bucket(object.bucket())
                        .key(object.objectKey())
                        .build()
        );
        return new StorageObjectMetadata(
                object.objectKey(),
                head.contentLength(),
                head.contentType(),
                head.eTag(),
                head.lastModified()
        );
    }

    @Override
    public String createDownloadUrl(StorageObject object, Duration expiration) {
        return presigner().presignGetObject(b -> b
                .getObjectRequest(r -> r
                        .bucket(object.bucket())
                        .key(object.objectKey())
                        .build())
                .signatureDuration(expiration)
                .build()
        ).url().toString();
    }

    // ── lazy initialisation ───────────────────────────────────────────────────

    private S3Client client() {
        if (client == null) {
            MediaProperties.R2Config r2 = props.getProvider().getR2();
            client = S3Client.builder()
                    .endpointOverride(URI.create(r2.getEndpoint()))
                    .region(Region.of("auto"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())
                    ))
                    .build();
        }
        return client;
    }

    private S3Presigner presigner() {
        if (presigner == null) {
            MediaProperties.R2Config r2 = props.getProvider().getR2();
            presigner = S3Presigner.builder()
                    .endpointOverride(URI.create(r2.getEndpoint()))
                    .region(Region.of("auto"))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(r2.getAccessKeyId(), r2.getSecretAccessKey())
                    ))
                    .build();
        }
        return presigner;
    }

    private String buildObjectKey(UploadRequest request) {
        String ext = extractExtension(request.filename());
        return String.format("%s/%s/%s/%s%s",
                request.ownerType().name().toLowerCase(),
                request.ownerId(),
                UUID.randomUUID(),
                "original",
                ext.isBlank() ? "" : "." + ext
        );
    }

    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase() : "";
    }
}
