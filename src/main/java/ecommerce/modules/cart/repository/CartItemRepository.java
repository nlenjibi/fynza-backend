package ecommerce.modules.cart.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.cart.entity.CartItem;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends BaseRepository<CartItem, UUID> {
    List<CartItem> findByCartId(UUID cartId);
    void deleteByCartId(UUID cartId);
    Optional<CartItem> findByCartIdAndId(UUID cartId, UUID cartItemId);
    Optional<CartItem> findByCartIdAndProductId(UUID cartId, UUID productId);
}
