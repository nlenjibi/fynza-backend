package ecommerce.modules.inventory.repository;

import ecommerce.modules.inventory.entity.InventorySummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InventorySummaryViewRepository
        extends JpaRepository<InventorySummaryView, Long>, JpaSpecificationExecutor<InventorySummaryView> {

    Page<InventorySummaryView> findBySellerIdAndIsActiveTrue(Long sellerId, Pageable pageable);
}
