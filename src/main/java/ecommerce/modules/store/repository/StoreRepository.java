package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.Store;
import ecommerce.modules.store.enums.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long>, JpaSpecificationExecutor<Store> {

    Optional<Store> findByPublicId(UUID publicId);

    Optional<Store> findBySlug(String slug);

    Optional<Store> findBySellerIdAndIsActiveTrue(Long sellerId);

    boolean existsBySlug(String slug);

    boolean existsBySellerIdAndStatusNot(Long sellerId, StoreStatus status);
}
