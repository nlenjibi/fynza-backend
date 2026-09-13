package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.InventoryReservation;
import ecommerce.modules.inventory.enums.InventoryReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservation, Long> {

    Optional<InventoryReservation> findByPublicId(UUID publicId);

    List<InventoryReservation> findByInventoryIdAndStatus(Long inventoryId, InventoryReservationStatus status);

    /**
     * Finds ACTIVE reservations whose expiry has passed — used by the scheduled expiry job.
     */
    @Query("""
            SELECT r FROM InventoryReservation r
            WHERE r.status = 'ACTIVE'
              AND r.expiresAt IS NOT NULL
              AND r.expiresAt < :now
            """)
    List<InventoryReservation> findExpiredActive(@Param("now") Instant now);
}
