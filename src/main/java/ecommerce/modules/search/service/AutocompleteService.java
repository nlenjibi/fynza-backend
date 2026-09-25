package ecommerce.modules.search.service;

import ecommerce.modules.search.dto.SearchSuggestionResponse;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class AutocompleteService {

    private final EntityManager em;

    @Transactional(readOnly = true)
    public List<SearchSuggestionResponse> suggest(String prefix, int limit) {
        if (prefix == null || prefix.isBlank() || prefix.length() < 2) return List.of();
        int safeLimit = Math.max(1, Math.min(limit, 10));
        String pattern = prefix.trim().toLowerCase() + "%";

        Set<SearchSuggestionResponse> suggestions = new LinkedHashSet<>();

        // Product name suggestions
        @SuppressWarnings("unchecked")
        List<String> names = em.createNativeQuery("""
                SELECT DISTINCT name FROM products
                WHERE LOWER(name) LIKE :pattern
                  AND status = 'ACTIVE' AND visibility = 'PUBLIC' AND is_active = TRUE
                ORDER BY name LIMIT :limit
                """)
                .setParameter("pattern", pattern)
                .setParameter("limit", safeLimit)
                .getResultList();
        names.forEach(n -> suggestions.add(new SearchSuggestionResponse(n, "PRODUCT")));

        // Brand suggestions (if still room)
        if (suggestions.size() < safeLimit) {
            @SuppressWarnings("unchecked")
            List<String> brands = em.createNativeQuery("""
                    SELECT DISTINCT brand FROM products
                    WHERE LOWER(brand) LIKE :pattern
                      AND status = 'ACTIVE' AND visibility = 'PUBLIC' AND is_active = TRUE
                      AND brand IS NOT NULL
                    ORDER BY brand LIMIT :limit
                    """)
                    .setParameter("pattern", pattern)
                    .setParameter("limit", safeLimit - suggestions.size())
                    .getResultList();
            brands.forEach(b -> suggestions.add(new SearchSuggestionResponse(b, "BRAND")));
        }

        // Category suggestions (if still room)
        if (suggestions.size() < safeLimit) {
            @SuppressWarnings("unchecked")
            List<String> categories = em.createNativeQuery("""
                    SELECT DISTINCT name FROM categories
                    WHERE LOWER(name) LIKE :pattern
                      AND is_active = TRUE
                    ORDER BY name LIMIT :limit
                    """)
                    .setParameter("pattern", pattern)
                    .setParameter("limit", safeLimit - suggestions.size())
                    .getResultList();
            categories.forEach(c -> suggestions.add(new SearchSuggestionResponse(c, "CATEGORY")));
        }

        return new ArrayList<>(suggestions).subList(0, Math.min(suggestions.size(), safeLimit));
    }
}
