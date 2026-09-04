package ecommerce.modules.cart.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.cart.entity.StockReservation;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends BaseRepository<StockReservation, UUID> {
    List<StockReservation> findByExpiresAtLessThan(LocalDateTime now);
    Optional<StockReservation> findByCartItemId(UUID cartItemId);
}
