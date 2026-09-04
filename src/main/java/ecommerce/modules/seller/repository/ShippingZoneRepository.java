package ecommerce.modules.seller.repository;

import ecommerce.modules.seller.entity.ShippingZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShippingZoneRepository extends JpaRepository<ShippingZone, Long> {
    List<ShippingZone> findBySellerIdAndIsActiveTrue(UUID sellerId);
    List<ShippingZone> findBySellerId(UUID sellerId);
}
