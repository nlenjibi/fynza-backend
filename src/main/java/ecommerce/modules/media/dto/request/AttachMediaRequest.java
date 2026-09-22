package ecommerce.modules.media.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachMediaRequest {

    @NotNull
    private UUID mediaAssetId;

    @Builder.Default
    private int sortOrder = 0;

    @Builder.Default
    private boolean isPrimary = false;

    private String altText;
}
