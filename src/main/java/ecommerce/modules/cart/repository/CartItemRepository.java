package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    void deleteByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndId(Long cartId, Long cartItemId);
    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);
    Optional<CartItem> findByPublicId(UUID publicId);
    Optional<CartItem> findByCartIdAndPublicId(Long cartId, UUID publicId);
    Optional<CartItem> findByCartIdAndProduct_PublicId(Long cartId, UUID productPublicId);
}
