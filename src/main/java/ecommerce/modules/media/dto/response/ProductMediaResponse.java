package ecommerce.modules.media.dto.response;

import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.ProductMedia;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductMediaResponse {

    private Long    id;
    private UUID    productId;
    private UUID    mediaAssetPublicId;
    private int     sortOrder;
    private boolean isPrimary;
    private String  altText;
    private MediaAssetResponse asset;
    private Instant createdAt;

    public static ProductMediaResponse from(ProductMedia pm, MediaAsset asset) {
        return ProductMediaResponse.builder()
                .id(pm.getId())
                .productId(pm.getProductId())
                .mediaAssetPublicId(asset.getPublicId())
                .sortOrder(pm.getSortOrder() != null ? pm.getSortOrder() : 0)
                .isPrimary(Boolean.TRUE.equals(pm.getIsPrimary()))
                .altText(pm.getAltText())
                .asset(MediaAssetResponse.from(asset))
                .createdAt(pm.getCreatedAt())
                .build();
    }
}
