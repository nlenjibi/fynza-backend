package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByShipment_PublicIdOrderByOccurredAtDesc(UUID shipmentPublicId);
}
