package ecommerce.modules.media.provider;

import ecommerce.modules.media.enums.ProviderType;
import ecommerce.modules.media.enums.UploadMethod;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record UploadSession(
        UUID        sessionId,
        ProviderType provider,
        UploadMethod uploadMethod,
        String       objectKey,
        String       uploadUrl,
        Map<String, String> requiredHeaders,
        Instant      expiresAt
) {}
