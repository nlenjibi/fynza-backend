package ecommerce.modules.seller.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.seller.entity.ShippingZone;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShippingZoneRepository extends BaseRepository<ShippingZone, UUID> {
    List<ShippingZone> findBySellerIdAndIsActiveTrue(UUID sellerId);
    List<ShippingZone> findBySellerId(UUID sellerId);
}
