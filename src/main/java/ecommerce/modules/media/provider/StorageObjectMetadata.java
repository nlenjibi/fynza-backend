package ecommerce.modules.media.provider;

import java.time.Instant;

public record StorageObjectMetadata(
        String  objectKey,
        long    contentLength,
        String  contentType,
        String  etag,
        Instant lastModified
) {}
