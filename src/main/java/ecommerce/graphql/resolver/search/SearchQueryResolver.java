package ecommerce.graphql.resolver.search;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.search.dto.ProductSearchPage;
import ecommerce.modules.search.dto.ProductSearchRequest;
import ecommerce.modules.search.dto.SearchHistoryResponse;
import ecommerce.modules.search.dto.SearchIndexStatusResponse;
import ecommerce.modules.search.dto.SearchSuggestionResponse;
import ecommerce.modules.search.service.AutocompleteService;
import ecommerce.modules.search.service.ProductSearchService;
import ecommerce.modules.search.service.SearchAnalyticsService;
import ecommerce.modules.search.service.SearchHistoryService;
import ecommerce.modules.search.service.SearchIndexService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SearchQueryResolver {

    private final ProductSearchService  productSearchService;
    private final AutocompleteService   autocompleteService;
    private final SearchHistoryService  historyService;
    private final SearchIndexService    indexService;
    private final SearchAnalyticsService analyticsService;

    @QueryMapping
    public ProductSearchPage productSearch(
            @Argument Map<String, Object> input,
            @AuthenticationPrincipal UserPrincipal principal) {

        String  query        = (String)  input.get("query");
        String  categoryIdStr= (String)  input.get("categoryId");
        String  brand        = (String)  input.get("brand");
        Object  minPriceRaw  = input.get("minPrice");
        Object  maxPriceRaw  = input.get("maxPrice");
        Integer minRating    = input.get("minRating") != null ? ((Number) input.get("minRating")).intValue() : null;
        String  availability = (String)  input.get("availability");
        String  sort         = (String)  input.get("sort");
        int     page         = input.get("page") != null ? ((Number) input.get("page")).intValue() : 0;
        int     size         = input.get("size") != null ? ((Number) input.get("size")).intValue() : 20;

        UUID       categoryId = categoryIdStr != null ? UUID.fromString(categoryIdStr) : null;
        BigDecimal minPrice   = minPriceRaw   != null ? new BigDecimal(minPriceRaw.toString()) : null;
        BigDecimal maxPrice   = maxPriceRaw   != null ? new BigDecimal(maxPriceRaw.toString()) : null;

        ProductSearchRequest request = new ProductSearchRequest(
                query, categoryId, brand, minPrice, maxPrice, minRating, availability, sort, page, size);

        UUID userId = principal != null ? principal.getId() : null;
        return productSearchService.search(request, userId);
    }

    @QueryMapping
    public List<SearchSuggestionResponse> searchSuggestions(
            @Argument String query,
            @Argument Integer limit) {
        return autocompleteService.suggest(query, limit != null ? limit : 10);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<SearchHistoryResponse> searchHistory(
            @Argument Integer limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        return historyService.getHistory(principal.getId(), limit != null ? limit : 20);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SearchIndexStatusResponse searchIndexStatus() {
        return indexService.getStatus();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> searchAnalyticsSummary(@Argument Integer days) {
        int d = days != null ? days : 30;
        List<Object[]> top  = analyticsService.getMostPopularSearches(10);
        double zeroRate     = analyticsService.getZeroResultRate(d);
        long totalSearches  = analyticsService.getTopSearches(d, org.springframework.data.domain.Pageable.ofSize(1))
                                               .getTotalElements();

        List<Map<String, Object>> topMapped = top.stream()
                .map(row -> Map.<String, Object>of(
                        "query",        row[0],
                        "searchCount",  ((Number) row[1]).intValue(),
                        "clickCount",   0,
                        "isZeroResults", false))
                .toList();

        return Map.of(
                "topSearches",    topMapped,
                "zeroResultRate", zeroRate,
                "totalSearches",  (int) totalSearches
        );
    }
}
