package ecommerce.modules.pricing.repository;

import ecommerce.modules.pricing.entity.Price;
import ecommerce.modules.pricing.enums.PriceStatus;
import ecommerce.modules.pricing.enums.SupportedCurrency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceRepository extends JpaRepository<Price, Long>, JpaSpecificationExecutor<Price> {

    Optional<Price> findByPublicId(UUID publicId);

    Optional<Price> findByPublicIdAndCreatedBy(UUID publicId, UUID createdBy);

    Page<Price> findByProductIdAndCreatedBy(UUID productId, UUID createdBy, Pageable pageable);

    List<Price> findAllByProductId(UUID productId);

    List<Price> findAllByVariantId(UUID variantId);

    boolean existsByPublicId(UUID publicId);

    /**
     * Finds the currently effective price for a product (no variant) in a given price list and currency.
     * Validity is evaluated at query time so scheduling works without a cron job.
     */
    @Query("""
            SELECT p FROM Price p
            WHERE p.priceList.id = :priceListId
              AND p.productId   = :productId
              AND p.variantId   IS NULL
              AND p.currency    = :currency
              AND p.isActive    = TRUE
              AND p.status      IN ('ACTIVE', 'SCHEDULED')
              AND (p.validFrom  IS NULL OR p.validFrom  <= :now)
              AND (p.validUntil IS NULL OR p.validUntil >  :now)
            ORDER BY p.validFrom DESC NULLS LAST
            """)
    List<Price> findEffectivePricesForProduct(
            @Param("priceListId") Long priceListId,
            @Param("productId")   UUID productId,
            @Param("currency")    SupportedCurrency currency,
            @Param("now")         Instant now);

    /**
     * Finds the currently effective price for a specific variant.
     */
    @Query("""
            SELECT p FROM Price p
            WHERE p.priceList.id = :priceListId
              AND p.productId   = :productId
              AND p.variantId   = :variantId
              AND p.currency    = :currency
              AND p.isActive    = TRUE
              AND p.status      IN ('ACTIVE', 'SCHEDULED')
              AND (p.validFrom  IS NULL OR p.validFrom  <= :now)
              AND (p.validUntil IS NULL OR p.validUntil >  :now)
            ORDER BY p.validFrom DESC NULLS LAST
            """)
    List<Price> findEffectivePricesForVariant(
            @Param("priceListId") Long priceListId,
            @Param("productId")   UUID productId,
            @Param("variantId")   UUID variantId,
            @Param("currency")    SupportedCurrency currency,
            @Param("now")         Instant now);

    /**
     * Checks whether an ACTIVE price already exists for the same scope (to enforce the overlap constraint).
     */
    @Query("""
            SELECT COUNT(p) > 0 FROM Price p
            WHERE p.priceList.id = :priceListId
              AND p.productId    = :productId
              AND p.currency     = :currency
              AND p.status       = 'ACTIVE'
              AND p.isActive     = TRUE
              AND (:variantId    IS NULL AND p.variantId IS NULL
                   OR p.variantId = :variantId)
              AND p.id           <> :excludeId
            """)
    boolean existsActiveOverlap(
            @Param("priceListId") Long priceListId,
            @Param("productId")   UUID productId,
            @Param("variantId")   UUID variantId,
            @Param("currency")    SupportedCurrency currency,
            @Param("excludeId")   Long excludeId);

    Page<Price> findByStatus(PriceStatus status, Pageable pageable);
}
