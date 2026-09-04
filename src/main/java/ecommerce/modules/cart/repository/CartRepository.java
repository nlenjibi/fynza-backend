package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUser_PublicId(UUID publicId);

    @Query("SELECT c FROM Cart c LEFT JOIN FETCH c.items WHERE c.user.publicId = :publicId")
    Optional<Cart> findByUserPublicIdWithItems(@Param("publicId") UUID publicId);

    // Abandoned cart queries
    @Query("SELECT c FROM Cart c WHERE c.isCheckedOut = false " +
           "AND c.isAbandoned = false " +
           "AND (c.lastActivityAt < :cutoffDate OR c.lastActivityAt IS NULL) " +
           "AND c.createdAt < :cutoffDate")
    List<Cart> findCartsNotCheckedOutAndNotAbandoned(@Param("cutoffDate") LocalDateTime cutoffDate);

    @Query("SELECT c FROM Cart c WHERE c.isAbandoned = true AND c.abandonedAt < :cutoffDate")
    List<Cart> findAbandonedCartsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);
}
