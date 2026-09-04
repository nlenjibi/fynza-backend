package ecommerce.modules.product.async;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageResult {

    private UUID imageId;
    private String productId;
    private ImageStatus status;
    private List<String> generatedUrls;
    private String errorMessage;

    public enum ImageStatus {
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public static ImageResult processing(UUID imageId, String productId) {
        return ImageResult.builder()
                .imageId(imageId)
                .productId(productId)
                .status(ImageStatus.PROCESSING)
                .build();
    }
}
