package ecommerce.modules.media.dto.request;

import ecommerce.modules.media.enums.MediaOwnerType;
import ecommerce.modules.media.enums.MediaType;
import ecommerce.modules.media.enums.MediaVisibility;
import jakarta.validation.constraints.*;

import java.util.UUID;

public record InitiateUploadRequest(

        @NotBlank
        String filename,

        @NotBlank
        String contentType,

        @Positive
        long size,

        @Min(1) @Max(8000)
        Integer width,

        @Min(1) @Max(8000)
        Integer height,

        @NotNull
        MediaType mediaType,

        @NotNull
        MediaOwnerType ownerType,

        @NotNull
        UUID ownerId,

        MediaVisibility visibility
) {}
