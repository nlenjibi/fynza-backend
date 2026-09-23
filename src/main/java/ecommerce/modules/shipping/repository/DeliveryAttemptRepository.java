package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {

    List<DeliveryAttempt> findByShipmentIdOrderByAttemptNumberAsc(UUID shipmentId);

    Optional<DeliveryAttempt> findByPublicId(UUID publicId);

    int countByShipmentId(UUID shipmentId);
}
