package ecommerce.modules.media.service;

import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.ProductMediaItemResponse;
import ecommerce.modules.media.dto.response.StorageQuotaResponse;

import java.util.List;
import java.util.UUID;

public interface MediaQueryService {

    MediaAssetResponse getAsset(UUID publicId, UUID userId);

    List<ProductMediaItemResponse> getProductMedia(UUID productId);

    StorageQuotaResponse getStorageQuota(UUID userId);
}
