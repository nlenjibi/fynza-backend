package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    List<CartItem> findByCartId(Long cartId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") Long cartId);

    Optional<CartItem> findByPublicId(UUID publicId);

    Optional<CartItem> findByCartIdAndPublicId(Long cartId, UUID publicId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productId = :productId AND ci.variantId IS NULL")
    Optional<CartItem> findByCartIdAndProductIdNoVariant(@Param("cartId") Long cartId, @Param("productId") UUID productId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productId = :productId AND ci.variantId = :variantId")
    Optional<CartItem> findByCartIdAndProductIdAndVariantId(@Param("cartId") Long cartId, @Param("productId") UUID productId, @Param("variantId") UUID variantId);

    default Optional<CartItem> findByCartIdAndProductAndVariant(Long cartId, UUID productId, UUID variantId) {
        if (variantId == null) {
            return findByCartIdAndProductIdNoVariant(cartId, productId);
        }
        return findByCartIdAndProductIdAndVariantId(cartId, productId, variantId);
    }

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.priceChanged = true")
    List<CartItem> findPriceChangedItems(@Param("cartId") Long cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId ORDER BY ci.storeId ASC, ci.createdAt ASC")
    List<CartItem> findByCartIdOrderByStore(@Param("cartId") Long cartId);
}
