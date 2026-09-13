package ecommerce.modules.wishlist.repository;

import ecommerce.modules.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByUser_PublicIdAndProduct_Id(UUID userPublicId, UUID productId);

    boolean existsByUser_PublicIdAndProduct_Id(UUID userPublicId, UUID productId);

    List<WishlistItem> findByUser_PublicIdOrderByCreatedAtDesc(UUID userPublicId);

    Page<WishlistItem> findByUser_PublicId(UUID userPublicId, Pageable pageable);

    // Price-drop detection moved to pricing module — returns empty until wired
    default List<WishlistItem> findItemsWithPriceDrops(UUID userPublicId) {
        return Collections.emptyList();
    }

    // Price/savings aggregation moved to pricing module — returns zeros until wired
    default Object[] findTotalValueAndSavings(UUID userPublicId) {
        return new Object[]{BigDecimal.ZERO, BigDecimal.ZERO};
    }

    long countByUser_PublicId(UUID userPublicId);

    @Modifying
    @Query("DELETE FROM WishlistItem w WHERE w.user.publicId = :userPublicId")
    int deleteByUser_PublicId(@Param("userPublicId") UUID userPublicId);
}
