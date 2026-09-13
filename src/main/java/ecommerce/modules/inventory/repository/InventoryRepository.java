package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InventoryRepository extends JpaRepository<Inventory, Long>, JpaSpecificationExecutor<Inventory> {

    Optional<Inventory> findByPublicId(UUID publicId);

    Optional<Inventory> findByPublicIdAndSellerId(UUID publicId, Long sellerId);

    Optional<Inventory> findByProductIdAndLocationIdAndVariantIdIsNull(UUID productId, Long locationId);

    Optional<Inventory> findByProductIdAndVariantIdAndLocationId(UUID productId, UUID variantId, Long locationId);

    List<Inventory> findByProductId(UUID productId);

    List<Inventory> findByProductIdAndIsActiveTrue(UUID productId);

    Page<Inventory> findBySellerId(Long sellerId, Pageable pageable);

    Page<Inventory> findBySellerIdAndIsActiveTrue(Long sellerId, Pageable pageable);

    boolean existsByPublicId(UUID publicId);

    /**
     * Atomically increments reservedQuantity only when sufficient stock is available.
     * Returns 1 on success, 0 when stock is insufficient (caller must throw InsufficientStockException).
     */
    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.reservedQuantity = i.reservedQuantity + :qty,
                i.updatedAt = :now
            WHERE i.id = :id
              AND (i.onHandQuantity - i.reservedQuantity) >= :qty
              AND i.isActive = TRUE
            """)
    int atomicReserve(@Param("id") Long id, @Param("qty") int qty, @Param("now") Instant now);

    /**
     * Atomically decrements reservedQuantity on reservation release.
     */
    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.reservedQuantity = i.reservedQuantity - :qty,
                i.updatedAt = :now
            WHERE i.id = :id
              AND i.reservedQuantity >= :qty
            """)
    int atomicRelease(@Param("id") Long id, @Param("qty") int qty, @Param("now") Instant now);

    /**
     * Atomically commits a reservation on payment success: decrements both onHandQuantity and reservedQuantity.
     */
    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.onHandQuantity  = i.onHandQuantity  - :qty,
                i.reservedQuantity = i.reservedQuantity - :qty,
                i.updatedAt = :now
            WHERE i.id = :id
              AND i.onHandQuantity  >= :qty
              AND i.reservedQuantity >= :qty
            """)
    int atomicCommit(@Param("id") Long id, @Param("qty") int qty, @Param("now") Instant now);

    /**
     * Adjusts onHandQuantity by a signed delta (positive = add, negative = remove).
     */
    @Modifying
    @Query("""
            UPDATE Inventory i
            SET i.onHandQuantity = i.onHandQuantity + :delta,
                i.updatedAt = :now
            WHERE i.id = :id
              AND (i.onHandQuantity + :delta) >= 0
            """)
    int atomicAdjust(@Param("id") Long id, @Param("delta") int delta, @Param("now") Instant now);
}
