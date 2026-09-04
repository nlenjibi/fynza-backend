package ecommerce.modules.wishlist.repository;

import ecommerce.modules.wishlist.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByPublicId(UUID publicId);

    Optional<Wishlist> findByUserId(Long userId);
}
