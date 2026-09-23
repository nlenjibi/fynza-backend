package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ReturnShipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReturnShipmentRepository extends JpaRepository<ReturnShipment, Long> {
    Optional<ReturnShipment> findByPublicId(UUID publicId);
    List<ReturnShipment> findByOriginalShipmentId(UUID originalShipmentId);
    Optional<ReturnShipment> findByTrackingNumber(String trackingNumber);
}
