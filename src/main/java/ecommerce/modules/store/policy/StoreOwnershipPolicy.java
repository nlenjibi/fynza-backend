package ecommerce.modules.store.policy;

import ecommerce.common.exception.ForbiddenException;
import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.policy.SellerOwnershipPolicy;
import ecommerce.modules.seller.repository.SellerRepository;
import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.exception.StoreNotFoundException;
import ecommerce.modules.store.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StoreOwnershipPolicy {

    private final StoreRepository storeRepository;
    private final SellerRepository sellerRepository;
    private final SellerOwnershipPolicy sellerOwnershipPolicy;

    public Seller resolveSeller(UUID actorUserId) {
        return sellerOwnershipPolicy.resolveOwn(actorUserId);
    }

    public Store resolveOwnStore(UUID actorUserId) {
        Seller seller = sellerOwnershipPolicy.resolveOwn(actorUserId);
        return storeRepository.findBySellerIdAndIsActiveTrue(seller.getId())
                .orElseThrow(() -> new StoreNotFoundException("No active store found for current seller"));
    }

    public Store assertOwns(UUID storePublicId, UUID actorUserId) {
        Seller seller = sellerOwnershipPolicy.resolveOwn(actorUserId);
        Store store = storeRepository.findByPublicId(storePublicId)
                .orElseThrow(() -> new StoreNotFoundException(storePublicId));
        if (!store.getSellerId().equals(seller.getId())) {
            throw new ForbiddenException("Access denied: store does not belong to requesting seller");
        }
        return store;
    }
}
