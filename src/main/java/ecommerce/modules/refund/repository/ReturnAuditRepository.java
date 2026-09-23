package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnAuditRepository extends JpaRepository<ReturnAudit, Long> {

    List<ReturnAudit> findByReturnIdOrderByCreatedAtAsc(UUID returnId);
}
