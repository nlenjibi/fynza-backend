package ecommerce.modules.media.provider;

public record StorageCapabilities(
        boolean directUpload,
        boolean presignedPut,
        boolean signedUpload,
        boolean resumableUpload,
        boolean multipartUpload,
        boolean signedDownload,
        boolean publicObjects,
        boolean imageTransformation,
        boolean cdn,
        boolean checksumVerification
) {}
