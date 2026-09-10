package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.StorePolicy;
import ecommerce.modules.store.enums.StorePolicyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StorePolicyRepository extends JpaRepository<StorePolicy, Long> {

    Optional<StorePolicy> findByPublicId(UUID publicId);

    List<StorePolicy> findByStoreIdAndIsActiveTrue(Long storeId);

    Optional<StorePolicy> findByStoreIdAndTypeAndIsActiveTrue(Long storeId, StorePolicyType type);

    boolean existsByStoreIdAndType(Long storeId, StorePolicyType type);
}
