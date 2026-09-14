package ecommerce.modules.media.dto.response;

import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.enums.MediaOwnerType;
import ecommerce.modules.media.enums.MediaStatus;
import ecommerce.modules.media.enums.MediaType;
import ecommerce.modules.media.enums.MediaVisibility;
import ecommerce.modules.media.enums.ProviderType;

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
) {
    public static MediaAssetResponse from(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getPublicId(),
                asset.getOwnerId(),
                asset.getOwnerType(),
                asset.getProvider(),
                asset.getOriginalFilename(),
                asset.getMimeType(),
                asset.getMediaType(),
                asset.getFileSize(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getVisibility(),
                asset.getStatus(),
                asset.getCdnUrl(),
                asset.getCreatedAt()
        );
    }
}
