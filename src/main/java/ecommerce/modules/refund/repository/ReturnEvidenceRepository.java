package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnEvidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnEvidenceRepository extends JpaRepository<ReturnEvidence, Long> {

    List<ReturnEvidence> findByReturnIdOrderByCreatedAtAsc(UUID returnId);

    List<ReturnEvidence> findByReturnItemId(UUID returnItemId);

    Optional<ReturnEvidence> findByPublicId(UUID publicId);
}
