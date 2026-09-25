package ecommerce.modules.search.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductSearchRequest(
        String query,
        UUID categoryId,
        String brand,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer minRating,
        String availability,
        String sort,
        int page,
        int size
) {
    public ProductSearchRequest {
        if (size <= 0 || size > 100) size = 20;
        if (page < 0) page = 0;
        if (sort == null || sort.isBlank()) sort = "RELEVANCE";
    }
}
