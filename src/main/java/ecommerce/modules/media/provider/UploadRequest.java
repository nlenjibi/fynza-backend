package ecommerce.modules.media.provider;

import ecommerce.modules.media.enums.MediaOwnerType;
import ecommerce.modules.media.enums.MediaType;
import ecommerce.modules.media.enums.MediaVisibility;

import java.util.UUID;

public record UploadRequest(
        UUID        userId,
        UUID        ownerId,
        MediaOwnerType ownerType,
        MediaType   mediaType,
        String      filename,
        String      mimeType,
        long        fileSize,
        Integer     width,
        Integer     height,
        MediaVisibility visibility
) {}
