package ecommerce.modules.wishlist.repository;

import ecommerce.modules.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByUser_PublicIdAndProduct_PublicId(UUID userPublicId, UUID productPublicId);

    boolean existsByUser_PublicIdAndProduct_PublicId(UUID userPublicId, UUID productPublicId);

    List<WishlistItem> findByUser_PublicIdOrderByCreatedAtDesc(UUID userPublicId);

    Page<WishlistItem> findByUser_PublicId(UUID userPublicId, Pageable pageable);

    @Query("SELECT w FROM WishlistItem w WHERE w.user.publicId = :userPublicId AND w.product.price < w.targetPrice")
    List<WishlistItem> findItemsWithPriceDrops(@Param("userPublicId") UUID userPublicId);

    @Query("SELECT COALESCE(SUM(w.product.price * w.desiredQuantity), 0), COALESCE(SUM((w.product.originalPrice - w.product.price) * w.desiredQuantity), 0) FROM WishlistItem w WHERE w.user.publicId = :userPublicId")
    Object[] findTotalValueAndSavings(@Param("userPublicId") UUID userPublicId);

    long countByUser_PublicId(UUID userPublicId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.user.publicId = :userPublicId")
    int deleteByUser_PublicId(@Param("userPublicId") UUID userPublicId);
}
