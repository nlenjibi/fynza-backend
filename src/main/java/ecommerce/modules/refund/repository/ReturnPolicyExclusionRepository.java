package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnPolicyExclusion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnPolicyExclusionRepository extends JpaRepository<ReturnPolicyExclusion, Long> {

    List<ReturnPolicyExclusion> findByPolicyId(UUID policyId);

    boolean existsByPolicyIdAndReferenceId(UUID policyId, UUID referenceId);
}
