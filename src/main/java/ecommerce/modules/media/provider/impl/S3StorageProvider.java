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

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(
        name = "fynza.media.provider.primary", havingValue = "S3")
@RequiredArgsConstructor
public class S3StorageProvider implements MediaStorageProvider {

    private static final StorageCapabilities CAPABILITIES = new StorageCapabilities(
            true, true, false, false, true, true, true, false, true, true
    );

    private final MediaProperties props;

    private S3Client    client;
    private S3Presigner presigner;

    @Override
    public ProviderType getProviderType() {
        return ProviderType.S3;
    }

    @Override
    public StorageCapabilities getCapabilities() {
        return CAPABILITIES;
    }

    @Override
    public UploadSession createUploadSession(UploadRequest request) {
        MediaProperties.S3Config s3 = props.getProvider().getS3();
        String objectKey = buildObjectKey(request);
        String uploadUrl = presigner().presignPutObject(
                PutObjectPresignRequest.builder()
                        .putObjectRequest(b -> b
                                .bucket(s3.getBucket())
                                .key(objectKey)
                                .contentType(request.mimeType())
                                .contentLength(request.fileSize())
                                .build())
                        .signatureDuration(Duration.ofSeconds(s3.getPresignedUrlExpirySeconds()))
                        .build()
        ).url().toString();

        return new UploadSession(
                UUID.randomUUID(),
                ProviderType.S3,
                UploadMethod.PRESIGNED_PUT,
                objectKey,
                uploadUrl,
                Map.of("Content-Type", request.mimeType()),
                Instant.now().plusSeconds(s3.getPresignedUrlExpirySeconds())
        );
    }

    @Override
    public UploadResult verifyUpload(UploadSession session) {
        MediaProperties.S3Config s3 = props.getProvider().getS3();
        HeadObjectResponse head = client().headObject(
                HeadObjectRequest.builder().bucket(s3.getBucket()).key(session.objectKey()).build()
        );
        String cdnUrl = s3.getCdnBaseUrl() != null
                ? s3.getCdnBaseUrl() + "/" + session.objectKey()
                : null;
        return new UploadResult(session.objectKey(), head.eTag(), head.checksumSHA256(), head.contentLength(), cdnUrl);
    }

    @Override
    public void deleteObject(StorageObject object) {
        client().deleteObject(DeleteObjectRequest.builder()
                .bucket(object.bucket()).key(object.objectKey()).build());
    }

    @Override
    public StorageObjectMetadata getMetadata(StorageObject object) {
        MediaProperties.S3Config s3 = props.getProvider().getS3();
        HeadObjectResponse head = client().headObject(
                HeadObjectRequest.builder().bucket(s3.getBucket()).key(object.objectKey()).build()
        );
        return new StorageObjectMetadata(object.objectKey(), head.contentLength(),
                head.contentType(), head.eTag(), head.lastModified());
    }

    @Override
    public String createDownloadUrl(StorageObject object, Duration expiration) {
        MediaProperties.S3Config s3 = props.getProvider().getS3();
        return presigner().presignGetObject(b -> b
                .getObjectRequest(r -> r.bucket(s3.getBucket()).key(object.objectKey()).build())
                .signatureDuration(expiration)
                .build()
        ).url().toString();
    }

    // ── lazy init ─────────────────────────────────────────────────────────────

    private S3Client client() {
        if (client == null) {
            MediaProperties.S3Config s3 = props.getProvider().getS3();
            client = S3Client.builder()
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s3.getAccessKeyId(), s3.getSecretAccessKey())
                    ))
                    .build();
        }
        return client;
    }

    private S3Presigner presigner() {
        if (presigner == null) {
            MediaProperties.S3Config s3 = props.getProvider().getS3();
            presigner = S3Presigner.builder()
                    .region(Region.of(s3.getRegion()))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(s3.getAccessKeyId(), s3.getSecretAccessKey())
                    ))
                    .build();
        }
        return presigner;
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
