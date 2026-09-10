package ecommerce.modules.store.repository;

import ecommerce.modules.store.entity.StoreSummaryView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StoreSummaryViewRepository extends JpaRepository<StoreSummaryView, Long>, JpaSpecificationExecutor<StoreSummaryView> {

    Optional<StoreSummaryView> findByPublicId(UUID publicId);

    Optional<StoreSummaryView> findBySlug(String slug);

    Optional<StoreSummaryView> findBySellerId(Long sellerId);
}
