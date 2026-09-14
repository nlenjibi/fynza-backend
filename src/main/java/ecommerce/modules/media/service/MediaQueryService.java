package ecommerce.modules.media.service;

import ecommerce.modules.media.dto.response.MediaAssetPage;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.ProductMediaItemResponse;
import ecommerce.modules.media.dto.response.StorageQuotaResponse;
import ecommerce.modules.media.dto.response.StorageUsageResponse;
import ecommerce.modules.media.enums.MediaStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface MediaQueryService {

    MediaAssetResponse getAsset(UUID publicId, UUID userId);

    List<ProductMediaItemResponse> getProductMedia(UUID productId);

    StorageQuotaResponse getStorageQuota(UUID userId);

    MediaAssetPage getMyAssets(UUID userId, Pageable pageable);

    MediaAssetPage getAssetsByStatus(MediaStatus status, Pageable pageable);

    MediaAssetPage adminListAssets(MediaStatus status, Pageable pageable);

    StorageUsageResponse getStorageUsage(UUID userId);
}
