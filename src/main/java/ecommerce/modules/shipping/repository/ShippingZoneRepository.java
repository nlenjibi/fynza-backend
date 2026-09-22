package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {
    Optional<ShippingZone> findByPublicId(UUID publicId);
    List<ShippingZone> findByIsActiveTrue();

    @Query(value = "SELECT * FROM shipping_zones z WHERE z.is_active = true AND :region = ANY(z.regions)", nativeQuery = true)
    List<ShippingZone> findActiveZonesByRegion(@Param("region") String region);
}
