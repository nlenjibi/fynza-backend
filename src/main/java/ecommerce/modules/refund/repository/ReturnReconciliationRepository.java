package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnReconciliation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnReconciliationRepository extends JpaRepository<ReturnReconciliation, Long> {

    List<ReturnReconciliation> findByReturnIdOrderByCreatedAtDesc(UUID returnId);
}
