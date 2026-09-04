package ecommerce.modules.wishlist.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.wishlist.entity.Wishlist;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends BaseRepository<Wishlist, UUID> {

    Optional<Wishlist> findByUserId(UUID userId);
}
