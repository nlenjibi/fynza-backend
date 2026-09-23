package ecommerce.modules.shipping.repository;

import ecommerce.modules.shipping.entity.ShippingReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ShippingReconciliationRepository extends JpaRepository<ShippingReconciliation, Long> {
    List<ShippingReconciliation> findByShipmentIdOrderByCreatedAtDesc(UUID shipmentId);
    List<ShippingReconciliation> findByResolvedFalseOrderByCreatedAtAsc();
    List<ShippingReconciliation> findByReconciliationDateAndResolvedFalse(LocalDate date);
    boolean existsByShipmentIdAndReconciliationDate(UUID shipmentId, LocalDate date);
}
