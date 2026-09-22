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

    @Query(value = """
            SELECT r.* FROM shipping_rates r
            JOIN shipping_methods m ON m.id = r.shipping_method_id
            JOIN shipping_zones z ON z.id = r.zone_id
            WHERE r.is_active = true
              AND z.is_active = true
              AND m.is_active = true
              AND :region = ANY(z.regions)
            """, nativeQuery = true)
    List<ShippingRate> findActiveRatesForRegion(@Param("region") String region);

    @Query(value = """
            SELECT r.* FROM shipping_rates r
            JOIN shipping_methods m ON m.id = r.shipping_method_id
            JOIN shipping_zones z ON z.id = r.zone_id
            WHERE r.is_active = true
              AND z.is_active = true
              AND m.is_active = true
            """, nativeQuery = true)
    List<ShippingRate> findAllActiveRates();

    List<ShippingRate> findByShippingMethod_PublicId(UUID methodPublicId);
}
