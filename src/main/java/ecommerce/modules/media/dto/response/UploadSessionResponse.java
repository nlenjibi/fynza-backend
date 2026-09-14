package ecommerce.modules.media.dto.response;

import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadMethod;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UploadSessionResponse(
        UUID                uploadId,
        UUID                mediaId,
        ProviderType        provider,
        UploadMethod        method,
        String              uploadUrl,
        Instant             expiresAt,
        Map<String, String> headers,
        String              objectKey
) {}
