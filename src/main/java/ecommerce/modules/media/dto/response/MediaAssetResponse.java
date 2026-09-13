package ecommerce.modules.media.dto.response;

import ecommerce.modules.media.enums.*;

import java.time.Instant;
import java.util.UUID;

public record MediaAssetResponse(
        UUID            publicId,
        UUID            ownerId,
        MediaOwnerType  ownerType,
        ProviderType    provider,
        String          originalFilename,
        String          mimeType,
        MediaType       mediaType,
        long            fileSize,
        Integer         width,
        Integer         height,
        MediaVisibility visibility,
        MediaStatus     status,
        String          cdnUrl,
        Instant         createdAt
) {}
