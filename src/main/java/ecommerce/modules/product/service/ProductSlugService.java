package ecommerce.modules.product.service;

import java.util.UUID;

public interface ProductSlugService {

    String generateSlug(String productName);

    String ensureUnique(String slug, UUID excludeProductId);
}
