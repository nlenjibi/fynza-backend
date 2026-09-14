package ecommerce.modules.media.dto.response;

import java.time.Instant;

public record SignedUrlResponse(
        String  url,
        Instant expiresAt
) {}
