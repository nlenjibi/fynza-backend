package ecommerce.modules.product.repository;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.enums.ProductVisibility;
import ecommerce.modules.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    boolean existsBySlug(String slug);
    boolean existsBySlugAndIdNot(String slug, UUID id);

    Optional<Product> findBySlug(String slug);
    Optional<Product> findByProductNumber(String productNumber);

    Page<Product> findByStoreIdAndIsActiveTrue(Long storeId, Pageable pageable);
    Page<Product> findBySellerIdAndIsActiveTrue(Long sellerId, Pageable pageable);

    Page<Product> findByStoreIdAndStatusAndIsActiveTrue(Long storeId, ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndVisibilityAndIsActiveTrue(
            ProductStatus status, ProductVisibility visibility, Pageable pageable);

    long countByStoreIdAndIsActiveTrue(Long storeId);
    long countBySellerIdAndIsActiveTrue(Long sellerId);
}
