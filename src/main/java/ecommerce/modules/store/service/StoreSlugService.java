package ecommerce.modules.store.service;

public interface StoreSlugService {

    String generateSlug(String storeName);

    void validateAndReserve(String slug, Long storeId);

    void rotateSlug(Long storeId, String newSlug);
}
