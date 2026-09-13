package ecommerce.modules.media.provider;

public record UploadResult(
        String objectKey,
        String etag,
        String checksum,
        long   fileSize,
        String cdnUrl
) {}
