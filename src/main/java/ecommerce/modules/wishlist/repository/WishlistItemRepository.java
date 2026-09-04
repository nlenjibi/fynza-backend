package ecommerce.modules.wishlist.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends BaseRepository<WishlistItem, UUID> {



    // Methods for user-based operations
    Optional<WishlistItem> findByUserIdAndProductId(UUID userId, UUID productId);
    
    boolean existsByUserIdAndProductId(UUID userId, UUID productId);
    
    List<WishlistItem> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Page<WishlistItem> findByUserId(UUID userId, Pageable pageable);
    
    @Query("SELECT w FROM WishlistItem w WHERE w.user.id = :userId AND w.product.price < w.targetPrice")
    List<WishlistItem> findItemsWithPriceDrops(@Param("userId") UUID userId);
    
    @Query("SELECT COALESCE(SUM(w.product.price * w.desiredQuantity), 0), COALESCE(SUM((w.product.originalPrice - w.product.price) * w.desiredQuantity), 0) FROM WishlistItem w WHERE w.user.id = :userId")
    Object[] findTotalValueAndSavings(@Param("userId") UUID userId);
    
    long countByUserId(UUID userId);
    
    int deleteByUserId(UUID userId);
}
