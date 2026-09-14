package ecommerce.modules.media.dto.response;

public record ProductMediaItemResponse(
        MediaAssetResponse mediaAsset,
        int                sortOrder,
        boolean            isPrimary,
        String             altText
) {}
