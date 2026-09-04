package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {
    List<StockReservation> findByExpiresAtLessThan(LocalDateTime now);
    Optional<StockReservation> findByCartItemId(Long cartItemId);
}
