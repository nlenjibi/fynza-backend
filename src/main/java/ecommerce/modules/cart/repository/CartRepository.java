package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {

    Optional<Cart> findByPublicId(UUID publicId);

    Optional<Cart> findByCartToken(String cartToken);

    @Query("SELECT c FROM Cart c WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveCartByUserId(@Param("userId") UUID userId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.userId = :userId AND c.status = 'ACTIVE'")
    Optional<Cart> findActiveCartByUserIdWithItems(@Param("userId") UUID userId);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.expiresAt < :now")
    List<Cart> findExpiredActiveCarts(@Param("now") Instant now);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.updatedAt < :cutoff AND c.isGuest = false")
    List<Cart> findInactiveUserCarts(@Param("cutoff") Instant cutoff);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ABANDONED' AND c.updatedAt < :cutoff")
    List<Cart> findAbandonedCartsOlderThan(@Param("cutoff") Instant cutoff);

    @Query("SELECT c FROM Cart c WHERE c.status = 'ACTIVE' AND c.isGuest = true AND c.expiresAt < :now")
    List<Cart> findExpiredGuestCarts(@Param("now") Instant now);

    default Optional<Cart> findByUserId(UUID userId) { return findActiveCartByUserId(userId); }
    default Optional<Cart> findByUserIdWithItems(UUID userId) { return findActiveCartByUserIdWithItems(userId); }
}
