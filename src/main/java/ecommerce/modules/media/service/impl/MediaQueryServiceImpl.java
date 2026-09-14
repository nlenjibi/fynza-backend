package ecommerce.modules.media.service.impl;

import ecommerce.modules.media.config.MediaProperties;
import ecommerce.modules.media.dto.response.MediaAssetPage;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.ProductMediaItemResponse;
import ecommerce.modules.media.dto.response.StorageQuotaResponse;
import ecommerce.modules.media.dto.response.StorageUsageResponse;
import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.StorageUsage;
import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.media.enums.MediaOwnerType;
import ecommerce.modules.media.enums.MediaStatus;
import ecommerce.modules.media.enums.MediaVisibility;
import ecommerce.modules.media.exception.MediaAssetNotFoundException;
import ecommerce.modules.media.repository.MediaAssetRepository;
import ecommerce.modules.media.repository.ProductMediaRepository;
import ecommerce.modules.media.repository.StorageUsageRepository;
import ecommerce.modules.media.service.MediaQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MediaQueryServiceImpl implements MediaQueryService {

    private final MediaAssetRepository    assetRepository;
    private final ProductMediaRepository  productMediaRepository;
    private final StorageUsageRepository  usageRepository;
    private final MediaProperties         props;

    @Override
    public MediaAssetResponse getAsset(UUID publicId, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(publicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(publicId));
        if (asset.getVisibility() != MediaVisibility.PUBLIC && !asset.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You do not have access to this media asset");
        }
        return toResponse(asset);
    }

    @Override
    public List<ProductMediaItemResponse> getProductMedia(UUID productId) {
        return productMediaRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(pm -> new ProductMediaItemResponse(
                        toResponse(pm.getMediaAsset()),
                        pm.getSortOrder(),
                        pm.getIsPrimary(),
                        pm.getAltText()
                ))
                .toList();
    }

    @Override
    public StorageQuotaResponse getStorageQuota(UUID userId) {
        StorageUsage usage = usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER).orElse(null);
        long used  = usage != null ? usage.getStorageBytes() : 0L;
        long count = usage != null ? usage.getObjectCount()  : 0L;
        long quota = props.getQuota().getDefaultSellerQuotaBytes();
        double pct = quota > 0 ? (double) used / quota * 100.0 : 0.0;
        return new StorageQuotaResponse(used, count, quota, pct);
    }

    @Override
    public MediaAssetPage getMyAssets(UUID userId, Pageable pageable) {
        return MediaAssetPage.from(
                assetRepository.findByUploadedByAndIsActiveTrue(userId, pageable)
                        .map(MediaAssetResponse::from)
        );
    }

    @Override
    public MediaAssetPage getAssetsByStatus(MediaStatus status, Pageable pageable) {
        return MediaAssetPage.from(
                assetRepository.findByStatus(status, pageable)
                        .map(MediaAssetResponse::from)
        );
    }

    @Override
    public MediaAssetPage adminListAssets(MediaStatus status, Pageable pageable) {
        if (status != null) {
            return MediaAssetPage.from(
                    assetRepository.findByStatus(status, pageable).map(MediaAssetResponse::from)
            );
        }
        return MediaAssetPage.from(
                assetRepository.findAll(pageable).map(MediaAssetResponse::from)
        );
    }

    @Override
    public StorageUsageResponse getStorageUsage(UUID userId) {
        long quota = props.getQuota().getDefaultSellerQuotaBytes();
        StorageUsage usage = usageRepository.findByOwnerIdAndOwnerType(userId, MediaOwnerType.USER)
                .orElseGet(() -> StorageUsage.builder()
                        .ownerId(userId)
                        .ownerType(MediaOwnerType.USER)
                        .storageBytes(0L)
                        .objectCount(0L)
                        .bandwidthBytes(0L)
                        .build());
        return StorageUsageResponse.from(usage, quota);
    }

    private MediaAssetResponse toResponse(MediaAsset asset) {
        return new MediaAssetResponse(
                asset.getPublicId(),
                asset.getOwnerId(),
                asset.getOwnerType(),
                asset.getProvider(),
                asset.getOriginalFilename(),
                asset.getMimeType(),
                asset.getMediaType(),
                asset.getFileSize(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getVisibility(),
                asset.getStatus(),
                asset.getCdnUrl(),
                asset.getCreatedAt()
        );
    }
}
