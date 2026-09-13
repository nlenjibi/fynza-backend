package ecommerce.modules.media.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AttachProductMediaRequest(

        @NotNull
        UUID mediaAssetId,

        boolean isPrimary,

        String altText,

        int sortOrder
) {}
