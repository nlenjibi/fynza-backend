package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByExpiresAtLessThan(LocalDateTime now);
    Optional<StockReservation> findByCartItemId(Long cartItemId);
    Optional<StockReservation> findByCartItem_PublicId(UUID cartItemPublicId);
    Optional<StockReservation> findByPublicId(UUID publicId);
}
