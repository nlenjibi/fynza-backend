package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnDisposition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnDispositionRepository extends JpaRepository<ReturnDisposition, Long> {

    List<ReturnDisposition> findByReturnIdOrderByCreatedAtAsc(UUID returnId);

    List<ReturnDisposition> findByReturnItemId(UUID returnItemId);
}
