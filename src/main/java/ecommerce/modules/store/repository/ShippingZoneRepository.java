package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {
    Optional<ShippingZone> findByPublicId(UUID publicId);
    List<ShippingZone> findBySellerIdAndIsActiveTrue(Long sellerId);
    List<ShippingZone> findBySellerId(Long sellerId);
}
