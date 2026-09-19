package ecommerce.modules.media.exception;

import ecommerce.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class UploadSessionNotFoundException extends ResourceNotFoundException {

    public UploadSessionNotFoundException(UUID publicId) {
        super("Upload session not found: " + publicId, "UPLOAD_SESSION_NOT_FOUND");
    }
}
