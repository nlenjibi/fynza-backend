package ecommerce.modules.product.service;

import ecommerce.modules.product.dto.request.CreateVariantRequest;
import ecommerce.modules.product.dto.request.UpdateVariantRequest;
import ecommerce.modules.product.dto.response.ProductVariantResponse;

import java.util.List;
import java.util.UUID;

public interface ProductVariantService {

    ProductVariantResponse createVariant(UUID actorUserId, UUID productId, CreateVariantRequest request);

    ProductVariantResponse updateVariant(UUID actorUserId, UUID productId, UUID variantId, UpdateVariantRequest request);

    void deleteVariant(UUID actorUserId, UUID productId, UUID variantId);

    List<ProductVariantResponse> getVariants(UUID productId);
}
