package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShipmentException;
import ecommerce.modules.shipping.enums.ExceptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentExceptionRepository extends JpaRepository<ShipmentException, Long> {
    List<ShipmentException> findByShipmentIdOrderByCreatedAtDesc(UUID shipmentId);
    Optional<ShipmentException> findByPublicId(UUID publicId);
    List<ShipmentException> findByStatusOrderByCreatedAtDesc(ExceptionStatus status);
    int countByShipmentIdAndStatus(UUID shipmentId, ExceptionStatus status);
}
