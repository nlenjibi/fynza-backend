package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShippingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingRateRepository extends JpaRepository<ShippingRate, Long> {
    Optional<ShippingRate> findByPublicId(UUID publicId);
    boolean existsByShippingMethod_IdAndZone_Id(Long methodId, Long zoneId);

    @Query("""
            SELECT r FROM ShippingRate r
            JOIN FETCH r.shippingMethod m
            JOIN FETCH m.carrier
            JOIN FETCH r.zone z
            WHERE r.isActive = true
              AND z.isActive = true
              AND m.isActive = true
              AND :region = ANY(z.regions)
            """)
    List<ShippingRate> findActiveRatesForRegion(@Param("region") String region);

    List<ShippingRate> findByShippingMethod_PublicId(UUID methodPublicId);
}
