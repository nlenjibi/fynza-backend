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
public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByCartId(UUID cartId);

    @Modifying
    @Query("DELETE FROM CartItem ci WHERE ci.cart.id = :cartId")
    void deleteByCartId(@Param("cartId") UUID cartId);

    Optional<CartItem> findByPublicId(UUID publicId);

    Optional<CartItem> findByCartIdAndPublicId(UUID cartId, UUID publicId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productId = :productId AND ci.variantId IS NULL")
    Optional<CartItem> findByCartIdAndProductIdNoVariant(@Param("cartId") UUID cartId, @Param("productId") UUID productId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.productId = :productId AND ci.variantId = :variantId")
    Optional<CartItem> findByCartIdAndProductIdAndVariantId(@Param("cartId") UUID cartId, @Param("productId") UUID productId, @Param("variantId") UUID variantId);

    default Optional<CartItem> findByCartIdAndProductAndVariant(UUID cartId, UUID productId, UUID variantId) {
        if (variantId == null) {
            return findByCartIdAndProductIdNoVariant(cartId, productId);
        }
        return findByCartIdAndProductIdAndVariantId(cartId, productId, variantId);
    }

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId AND ci.priceChanged = true")
    List<CartItem> findPriceChangedItems(@Param("cartId") UUID cartId);

    @Query("SELECT ci FROM CartItem ci WHERE ci.cart.id = :cartId ORDER BY ci.storeId ASC, ci.createdAt ASC")
    List<CartItem> findByCartIdOrderByStore(@Param("cartId") UUID cartId);
}
