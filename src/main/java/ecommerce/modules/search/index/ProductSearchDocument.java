package ecommerce.modules.search.index;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductSearchDocument(
        UUID productId,
        String name,
        String brand,
        String slug,
        String sku,
        String description,
        UUID categoryId,
        String categoryName,
        Long sellerId,
        Long storeId,
        BigDecimal price,
        String currency,
        String availability,
        Integer availableQuantity,
        BigDecimal averageRating,
        Integer reviewCount,
        String status,
        String visibility,
        Instant createdAt,
        Instant updatedAt
) {}
