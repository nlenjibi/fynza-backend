package ecommerce.modules.search.dto;

import java.time.Instant;

public record SearchIndexStatusResponse(
        long totalProducts,
        long indexedCount,
        Instant lastRebuiltAt,
        String status
) {}
