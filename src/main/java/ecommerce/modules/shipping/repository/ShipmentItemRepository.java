package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShipmentItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShipmentItemRepository extends JpaRepository<ShipmentItem, Long> {
    List<ShipmentItem> findByShipment_PublicId(UUID shipmentPublicId);
}
