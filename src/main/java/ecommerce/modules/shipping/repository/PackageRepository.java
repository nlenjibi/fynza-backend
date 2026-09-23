package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShipmentPackage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PackageRepository extends JpaRepository<ShipmentPackage, Long> {

    List<ShipmentPackage> findByShipmentIdAndIsActiveTrue(UUID shipmentId);

    Optional<ShipmentPackage> findByPublicId(UUID publicId);

    int countByShipmentId(UUID shipmentId);
}
