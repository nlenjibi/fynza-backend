package ecommerce.modules.media.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class MediaAssetNotFoundException extends ResourceNotFoundException {

    public MediaAssetNotFoundException(UUID publicId) {
        super("Media asset not found: " + publicId, "MEDIA_ASSET_NOT_FOUND");
    }
}
