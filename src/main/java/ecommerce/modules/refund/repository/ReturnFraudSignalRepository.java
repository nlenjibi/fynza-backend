package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnFraudSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnFraudSignalRepository extends JpaRepository<ReturnFraudSignal, Long> {

    List<ReturnFraudSignal> findByReturnIdOrderByCreatedAtDesc(UUID returnId);

    long countByCustomerIdAndCreatedAtAfter(UUID customerId, Instant after);

    long countByCustomerIdAndSignalTypeAndCreatedAtAfter(
            UUID customerId,
            ecommerce.modules.refund.enums.ReturnFraudSignalType signalType,
            Instant after);
}
