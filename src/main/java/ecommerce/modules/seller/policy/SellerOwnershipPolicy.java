package ecommerce.modules.seller.policy;

import ecommerce.modules.seller.entity.Seller;
import ecommerce.modules.seller.exception.SellerNotFoundException;
import ecommerce.modules.seller.repository.SellerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SellerOwnershipPolicy {

    private final SellerRepository sellerRepository;

    public Seller resolveOwn(UUID userId) {
        return sellerRepository.findByOwnerUserId(userId)
                .orElseThrow(() -> new SellerNotFoundException("No seller account found for user: " + userId));
    }

    public void assertOwns(UUID userId, Seller seller) {
        if (!seller.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied: not the seller owner");
        }
    }
}
