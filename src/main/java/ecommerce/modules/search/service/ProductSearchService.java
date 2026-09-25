package ecommerce.modules.search.service;

import ecommerce.modules.search.dto.ProductSearchPage;
import ecommerce.modules.search.dto.ProductSearchRequest;
import ecommerce.modules.search.dto.ProductSearchResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductSearchService {

    private static final String SELECT_SQL = """
            SELECT
                p.id,
                p.name,
                p.brand,
                p.slug,
                p.created_at,
                COALESCE(src.price, 0)              AS price,
                COALESCE(src.currency, 'GHS')        AS currency,
                COALESCE(src.availability, 'UNKNOWN') AS availability,
                COALESCE(src.average_rating, 0)      AS average_rating,
                COALESCE(src.review_count, 0)        AS review_count,
                COALESCE(src.category_name, '')      AS category_name
            FROM products p
            LEFT JOIN v_product_search_source src ON src.product_id = p.id
            WHERE p.status = 'ACTIVE'
              AND p.visibility = 'PUBLIC'
              AND p.is_active = TRUE
              AND (:query IS NULL OR :query = '' OR
                   to_tsvector('english',
                       COALESCE(p.name,'') || ' ' ||
                       COALESCE(p.brand,'') || ' ' ||
                       COALESCE(p.description,''))
                   @@ websearch_to_tsquery('english', :query))
              AND (:categoryId IS NULL OR src.category_id = CAST(:categoryId AS uuid))
              AND (:brand IS NULL OR LOWER(COALESCE(p.brand,'')) = LOWER(:brand))
              AND (:minPrice IS NULL OR src.price >= CAST(:minPrice AS numeric))
              AND (:maxPrice IS NULL OR src.price <= CAST(:maxPrice AS numeric))
              AND (:minRating IS NULL OR src.average_rating >= CAST(:minRating AS numeric))
              AND (:availability IS NULL OR src.availability = :availability)
            ORDER BY
              CASE WHEN :sort = 'PRICE_LOW_TO_HIGH' THEN src.price END ASC NULLS LAST,
              CASE WHEN :sort = 'PRICE_HIGH_TO_LOW' THEN src.price END DESC NULLS LAST,
              CASE WHEN :sort = 'NEWEST'            THEN p.created_at END DESC NULLS LAST,
              CASE WHEN :sort = 'RATING'            THEN src.average_rating END DESC NULLS LAST,
              CASE WHEN :sort = 'MOST_REVIEWED'     THEN src.review_count END DESC NULLS LAST,
              CASE WHEN (:sort = 'RELEVANCE' OR :sort IS NULL) AND (:query IS NOT NULL AND :query != '') THEN
                ts_rank(to_tsvector('english',
                    COALESCE(p.name,'') || ' ' ||
                    COALESCE(p.brand,'') || ' ' ||
                    COALESCE(p.description,'')),
                  websearch_to_tsquery('english', :query))
              END DESC NULLS LAST,
              p.created_at DESC
            LIMIT :size OFFSET :offset
            """;

    private static final String COUNT_SQL = """
            SELECT COUNT(*)
            FROM products p
            LEFT JOIN v_product_search_source src ON src.product_id = p.id
            WHERE p.status = 'ACTIVE'
              AND p.visibility = 'PUBLIC'
              AND p.is_active = TRUE
              AND (:query IS NULL OR :query = '' OR
                   to_tsvector('english',
                       COALESCE(p.name,'') || ' ' ||
                       COALESCE(p.brand,'') || ' ' ||
                       COALESCE(p.description,''))
                   @@ websearch_to_tsquery('english', :query))
              AND (:categoryId IS NULL OR src.category_id = CAST(:categoryId AS uuid))
              AND (:brand IS NULL OR LOWER(COALESCE(p.brand,'')) = LOWER(:brand))
              AND (:minPrice IS NULL OR src.price >= CAST(:minPrice AS numeric))
              AND (:maxPrice IS NULL OR src.price <= CAST(:maxPrice AS numeric))
              AND (:minRating IS NULL OR src.average_rating >= CAST(:minRating AS numeric))
              AND (:availability IS NULL OR src.availability = :availability)
            """;

    private final EntityManager em;
    private final SynonymService synonymService;
    private final SearchAnalyticsService analyticsService;

    @Transactional(readOnly = true)
    public ProductSearchPage search(ProductSearchRequest request, UUID userId) {
        String rawQuery = sanitize(request.query());
        String expandedQuery = rawQuery != null ? synonymService.expandQuery(rawQuery) : null;

        String categoryIdStr = request.categoryId() != null ? request.categoryId().toString() : null;
        String sort = request.sort();
        int offset = request.page() * request.size();

        Query select = em.createNativeQuery(SELECT_SQL)
                .setParameter("query", expandedQuery)
                .setParameter("categoryId", categoryIdStr)
                .setParameter("brand", request.brand())
                .setParameter("minPrice", request.minPrice())
                .setParameter("maxPrice", request.maxPrice())
                .setParameter("minRating", request.minRating())
                .setParameter("availability", request.availability())
                .setParameter("sort", sort)
                .setParameter("size", request.size())
                .setParameter("offset", offset);

        Query count = em.createNativeQuery(COUNT_SQL)
                .setParameter("query", expandedQuery)
                .setParameter("categoryId", categoryIdStr)
                .setParameter("brand", request.brand())
                .setParameter("minPrice", request.minPrice())
                .setParameter("maxPrice", request.maxPrice())
                .setParameter("minRating", request.minRating())
                .setParameter("availability", request.availability());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = select.getResultList();
        long total = ((Number) count.getSingleResult()).longValue();

        List<ProductSearchResult> items = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            items.add(mapRow(row));
        }

        int totalPages = (int) Math.ceil((double) total / request.size());
        boolean hasNext = request.page() < totalPages - 1;

        try {
            analyticsService.trackSearch(rawQuery != null ? rawQuery : "", items.size(),
                    sort, userId, null, null, null);
        } catch (Exception ex) {
            log.warn("Failed to track search analytics: {}", ex.getMessage());
        }

        return new ProductSearchPage(rawQuery, total, items, request.page(), totalPages, hasNext);
    }

    private ProductSearchResult mapRow(Object[] row) {
        UUID productId   = row[0] instanceof UUID u ? u : UUID.fromString(row[0].toString());
        String name      = (String) row[1];
        String brand     = (String) row[2];
        String slug      = (String) row[3];
        Instant createdAt = row[4] instanceof Instant i ? i
                : row[4] instanceof java.sql.Timestamp ts ? ts.toInstant() : null;
        BigDecimal price        = row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
        String currency         = (String) row[6];
        String availability     = (String) row[7];
        BigDecimal avgRating    = row[8] != null ? new BigDecimal(row[8].toString()) : BigDecimal.ZERO;
        Integer reviewCount     = row[9] != null ? ((Number) row[9]).intValue() : 0;
        String categoryName     = (String) row[10];
        return new ProductSearchResult(productId, name, brand, slug, price, currency,
                availability, avgRating, reviewCount, categoryName, createdAt);
    }

    private String sanitize(String query) {
        if (query == null || query.isBlank()) return null;
        String trimmed = query.trim();
        if (trimmed.length() > 200) trimmed = trimmed.substring(0, 200);
        return trimmed;
    }
}
