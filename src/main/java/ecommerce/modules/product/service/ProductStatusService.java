package ecommerce.modules.product.service;

import ecommerce.modules.product.dto.response.ProductResponse;

import java.util.UUID;

public interface ProductStatusService {

    ProductResponse submitForReview(UUID actorUserId, UUID productId);

    ProductResponse approveProduct(UUID adminUserId, UUID productId);

    ProductResponse rejectProduct(UUID adminUserId, UUID productId, String reason);

    ProductResponse publishProduct(UUID actorUserId, UUID productId);

    ProductResponse deactivateProduct(UUID actorUserId, UUID productId);

    ProductResponse suspendProduct(UUID adminUserId, UUID productId, String reason);

    ProductResponse restoreProduct(UUID adminUserId, UUID productId);
}
