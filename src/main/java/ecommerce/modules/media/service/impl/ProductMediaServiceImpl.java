package ecommerce.modules.media.service.impl;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.media.dto.request.AttachMediaRequest;
import ecommerce.modules.media.dto.request.ReorderMediaRequest;
import ecommerce.modules.media.dto.response.ProductMediaResponse;
import ecommerce.modules.media.entity.MediaAsset;
import ecommerce.modules.media.entity.ProductMedia;
import ecommerce.modules.media.exception.MediaAssetNotFoundException;
import ecommerce.modules.media.repository.MediaAssetRepository;
import ecommerce.modules.media.repository.ProductMediaMappingRepository;
import ecommerce.modules.media.service.ProductMediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductMediaServiceImpl implements ProductMediaService {

    private final MediaAssetRepository   assetRepository;
    private final ProductMediaMappingRepository productMediaRepository;

    @Override
    @Transactional
    public ProductMediaResponse attachMedia(UUID productId, AttachMediaRequest request, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(request.getMediaAssetId())
                .orElseThrow(() -> new MediaAssetNotFoundException(request.getMediaAssetId()));

        if (!asset.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You do not own this media asset");
        }

        if (request.isPrimary()) {
            productMediaRepository.clearPrimaryForProduct(productId);
        }

        ProductMedia pm = ProductMedia.builder()
                .productId(productId)
                .mediaAsset(asset)
                .sortOrder(request.getSortOrder())
                .isPrimary(request.isPrimary())
                .altText(request.getAltText())
                .build();
        pm = productMediaRepository.save(pm);

        log.info("Media asset={} attached to product={}", asset.getPublicId(), productId);
        return ProductMediaResponse.from(pm, asset);
    }

    @Override
    @Transactional
    public void detachMedia(UUID productId, UUID mediaAssetPublicId, UUID userId) {
        MediaAsset asset = assetRepository.findByPublicId(mediaAssetPublicId)
                .orElseThrow(() -> new MediaAssetNotFoundException(mediaAssetPublicId));

        if (!asset.getUploadedBy().equals(userId)) {
            throw new ForbiddenException("You do not own this media asset");
        }

        productMediaRepository.findByProductIdAndMediaAssetId(productId, asset.getId())
                .ifPresent(productMediaRepository::delete);

        log.info("Media asset={} detached from product={}", mediaAssetPublicId, productId);
    }

    @Override
    @Transactional
    public void reorderMedia(UUID productId, ReorderMediaRequest request, UUID userId) {
        List<ProductMedia> productMediaList =
                productMediaRepository.findByProductIdOrderBySortOrderAsc(productId);

        List<UUID> orderedIds = request.getOrderedMediaIds();
        for (int i = 0; i < orderedIds.size(); i++) {
            UUID assetPublicId = orderedIds.get(i);
            int sortOrder = i;
            productMediaList.stream()
                    .filter(pm -> pm.getMediaAsset().getPublicId().equals(assetPublicId))
                    .findFirst()
                    .ifPresent(pm -> {
                        pm.setSortOrder(sortOrder);
                        productMediaRepository.save(pm);
                    });
        }

        log.info("User {} reordered {} media items for product={}", userId, orderedIds.size(), productId);
    }

    @Override
    public List<ProductMediaResponse> getProductMedia(UUID productId) {
        return productMediaRepository.findByProductIdOrderBySortOrderAsc(productId).stream()
                .map(pm -> ProductMediaResponse.from(pm, pm.getMediaAsset()))
                .toList();
    }
}
