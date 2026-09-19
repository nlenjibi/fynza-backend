package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductSummaryView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductSummaryViewRepository
        extends JpaRepository<ProductSummaryView, UUID>, JpaSpecificationExecutor<ProductSummaryView> {

    Page<ProductSummaryView> findByStatusAndVisibilityAndIsActiveTrue(
            String status, String visibility, Pageable pageable);

    Page<ProductSummaryView> findByStoreIdAndIsActiveTrue(Long storeId, Pageable pageable);

    Page<ProductSummaryView> findBySellerIdAndIsActiveTrue(Long sellerId, Pageable pageable);
}
