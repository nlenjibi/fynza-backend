package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnInspection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnInspectionRepository extends JpaRepository<ReturnInspection, Long> {

    List<ReturnInspection> findByReturnIdOrderByCreatedAtAsc(UUID returnId);

    List<ReturnInspection> findByReturnItemId(UUID returnItemId);
}
