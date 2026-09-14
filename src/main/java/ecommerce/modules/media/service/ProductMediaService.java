package ecommerce.modules.media.service;

import ecommerce.modules.media.dto.request.AttachMediaRequest;
import ecommerce.modules.media.dto.request.ReorderMediaRequest;
import ecommerce.modules.media.dto.response.ProductMediaResponse;

import java.util.List;
import java.util.UUID;

public interface ProductMediaService {

    ProductMediaResponse       attachMedia(UUID productId, AttachMediaRequest request, UUID userId);

    void                       detachMedia(UUID productId, UUID mediaAssetPublicId, UUID userId);

    void                       reorderMedia(UUID productId, ReorderMediaRequest request, UUID userId);

    List<ProductMediaResponse> getProductMedia(UUID productId);
}
