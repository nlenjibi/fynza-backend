package ecommerce.modules.wishlist.repository;

import ecommerce.modules.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, UUID> {

    List<WishlistItem> findByWishlist_PublicId(UUID wishlistId);

    Optional<WishlistItem> findByPublicId(UUID publicId);

    Optional<WishlistItem> findByWishlist_PublicIdAndProductIdAndVariantId(
            UUID wishlistId, UUID productId, UUID variantId);

    boolean existsByWishlist_PublicIdAndProductIdAndVariantId(
            UUID wishlistId, UUID productId, UUID variantId);

    boolean existsByWishlist_CustomerIdAndProductIdAndVariantId(
            UUID customerId, UUID productId, UUID variantId);

    long countByWishlist_PublicId(UUID wishlistId);

    long countByWishlist_CustomerId(UUID customerId);

    @Modifying
    @Query("DELETE FROM WishlistItem wi WHERE wi.wishlist.publicId = :wishlistId")
    int deleteByWishlist_PublicId(@Param("wishlistId") UUID wishlistId);
}
