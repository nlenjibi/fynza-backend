package ecommerce.modules.product.service;

import ecommerce.modules.product.dto.SearchRequest;
import ecommerce.modules.product.dto.SearchResponse;

import java.util.List;
import java.util.UUID;

public interface SearchService {

    SearchResponse search(SearchRequest request);

    List<String> getSuggestions(String query, int limit);

    List<String> getTrending(int limit, String timeRange);

    List<ecommerce.modules.product.dto.ProductResponse> getPopularProducts(int limit, UUID categoryId);
}
