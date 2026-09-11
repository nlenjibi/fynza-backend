package ecommerce.modules.product.policy;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.exception.ProductNotFoundException;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.policy.StoreOwnershipPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ProductOwnershipPolicy {

    private final ProductRepository     productRepository;
    private final StoreOwnershipPolicy  storeOwnershipPolicy;

    public Product assertOwns(UUID productId, UUID actorUserId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        Store store = storeOwnershipPolicy.resolveOwnStore(actorUserId);

        if (!store.getId().equals(product.getStoreId())) {
            throw new ForbiddenException("Access denied: product does not belong to your store");
        }
        return product;
    }

    public Product assertOwnsInStore(UUID productId, UUID storePublicId, UUID actorUserId) {
        Store store = storeOwnershipPolicy.assertOwns(storePublicId, actorUserId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        if (!store.getId().equals(product.getStoreId())) {
            throw new ForbiddenException("Access denied: product does not belong to the specified store");
        }
        return product;
    }
}
