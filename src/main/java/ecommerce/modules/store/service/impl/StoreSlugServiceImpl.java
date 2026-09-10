package ecommerce.modules.store.service.impl;

import ecommerce.modules.store.entity.StoreSlugHistory;
import ecommerce.modules.store.exception.StoreSlugAlreadyTakenException;
import ecommerce.modules.store.repository.StoreRepository;
import ecommerce.modules.store.repository.StoreSlugHistoryRepository;
import ecommerce.modules.store.service.StoreSlugService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class StoreSlugServiceImpl implements StoreSlugService {

    private final StoreRepository storeRepository;
    private final StoreSlugHistoryRepository slugHistoryRepository;

    @Override
    public String generateSlug(String storeName) {
        String base = Normalizer.normalize(storeName, Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");

        if (!storeRepository.existsBySlug(base) && !slugHistoryRepository.existsBySlug(base)) {
            return base;
        }

        int suffix = 2;
        String candidate;
        do {
            candidate = base + "-" + suffix++;
        } while (storeRepository.existsBySlug(candidate) || slugHistoryRepository.existsBySlug(candidate));
        return candidate;
    }

    @Override
    @Transactional
    public void validateAndReserve(String slug, Long storeId) {
        if (storeRepository.existsBySlug(slug)) {
            throw new StoreSlugAlreadyTakenException(slug);
        }
        if (slugHistoryRepository.existsBySlug(slug)) {
            throw new StoreSlugAlreadyTakenException(slug);
        }
        slugHistoryRepository.save(
                StoreSlugHistory.builder()
                        .storeId(storeId)
                        .slug(slug)
                        .build()
        );
    }

    @Override
    @Transactional
    public void rotateSlug(Long storeId, String newSlug) {
        if (storeRepository.existsBySlug(newSlug)) {
            throw new StoreSlugAlreadyTakenException(newSlug);
        }
        slugHistoryRepository.save(
                StoreSlugHistory.builder()
                        .storeId(storeId)
                        .slug(newSlug)
                        .build()
        );
    }
}
