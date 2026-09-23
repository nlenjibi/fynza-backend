package ecommerce.modules.refund.repository;

import ecommerce.modules.refund.entity.ReturnPolicy;
import ecommerce.modules.refund.enums.ReturnPolicyScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReturnPolicyRepository extends JpaRepository<ReturnPolicy, Long> {

    Optional<ReturnPolicy> findByPublicId(UUID publicId);

    Optional<ReturnPolicy> findByScopeAndIsActiveTrue(ReturnPolicyScope scope);

    Optional<ReturnPolicy> findByScopeAndStoreIdAndIsActiveTrue(ReturnPolicyScope scope, UUID storeId);

    Optional<ReturnPolicy> findByScopeAndProductIdAndIsActiveTrue(ReturnPolicyScope scope, UUID productId);

    Optional<ReturnPolicy> findByScopeAndCategoryIdAndIsActiveTrue(ReturnPolicyScope scope, UUID categoryId);
}
