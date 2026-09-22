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

    @Query("SELECT z FROM ShippingZone z WHERE z.isActive = true AND :region = ANY(z.regions)")
    List<ShippingZone> findActiveZonesByRegion(@Param("region") String region);
}
