package ecommerce.modules.search.dto;

import java.util.List;

public record ProductSearchPage(
        String query,
        long total,
        List<ProductSearchResult> items,
        int currentPage,
        int totalPages,
        boolean hasNextPage
) {}
