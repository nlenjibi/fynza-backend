package ecommerce.modules.media.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteUploadRequest(

        @Positive
        long size,

        String etag,

        String checksum
) {}
