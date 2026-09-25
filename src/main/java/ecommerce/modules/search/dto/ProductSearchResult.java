package ecommerce.modules.search.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductSearchResult(
        UUID productId,
        String name,
        String brand,
        String slug,
        BigDecimal price,
        String currency,
        String availability,
        BigDecimal averageRating,
        Integer reviewCount,
        String categoryName,
        Instant createdAt
) {}
