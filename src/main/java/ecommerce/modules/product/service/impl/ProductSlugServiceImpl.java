package ecommerce.modules.product.service.impl;

import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.product.service.ProductSlugService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductSlugServiceImpl implements ProductSlugService {

    private final ProductRepository productRepository;

    @Override
    public String generateSlug(String productName) {
        String base = normalize(productName);
        if (!productRepository.existsBySlug(base)) {
            return base;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = base + "-" + suffix++;
        } while (productRepository.existsBySlug(candidate));
        return candidate;
    }

    @Override
    public String ensureUnique(String slug, UUID excludeProductId) {
        String base = normalize(slug);
        if (!productRepository.existsBySlugAndIdNot(base, excludeProductId)) {
            return base;
        }
        int suffix = 2;
        String candidate;
        do {
            candidate = base + "-" + suffix++;
        } while (productRepository.existsBySlugAndIdNot(candidate, excludeProductId));
        return candidate;
    }

    private String normalize(String input) {
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}
