package ecommerce.modules.wishlist.repository;

import ecommerce.modules.wishlist.entity.Wishlist;
import ecommerce.modules.wishlist.entity.WishlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {

    Optional<Wishlist> findByPublicId(UUID publicId);

    List<Wishlist> findByCustomerIdAndStatusNot(UUID customerId, WishlistStatus status);

    Optional<Wishlist> findByCustomerIdAndIsDefaultTrue(UUID customerId);

    Optional<Wishlist> findByGuestTokenHash(String hash);

    Optional<Wishlist> findByShareTokenHash(String hash);

    boolean existsByCustomerIdAndIsDefaultTrue(UUID customerId);

    long countByCustomerIdAndStatusNot(UUID customerId, WishlistStatus status);
}
