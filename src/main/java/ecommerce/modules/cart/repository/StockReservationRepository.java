package ecommerce.modules.cart.repository;

import ecommerce.modules.cart.entity.ReservationStatus;
import ecommerce.modules.cart.entity.StockReservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, UUID> {

    List<StockReservation> findByExpiresAtLessThan(Instant now);

    Optional<StockReservation> findByCartItemId(UUID cartItemId);

    Optional<StockReservation> findByPublicId(UUID publicId);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.cartId = :cartId")
    List<StockReservation> findByCartId(@Param("cartId") UUID cartId);

    @Modifying
    @Query("DELETE FROM StockReservation sr WHERE sr.cartId = :cartId")
    void deleteByCartId(@Param("cartId") UUID cartId);

    @Query("SELECT sr FROM StockReservation sr WHERE sr.status = :status AND sr.expiresAt < :now")
    List<StockReservation> findByStatusAndExpiresAtBefore(@Param("status") ReservationStatus status, @Param("now") Instant now);
}
