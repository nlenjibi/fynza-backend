package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShippingLabel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShippingLabelRepository extends JpaRepository<ShippingLabel, Long> {
    Optional<ShippingLabel> findByShipment_PublicId(UUID shipmentPublicId);
}
