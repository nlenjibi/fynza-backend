package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMediaRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdAndIsActiveTrueOrderBySortOrder(UUID productId);

    Optional<ProductMedia> findByProductIdAndIsPrimaryTrue(UUID productId);

    long countByProductIdAndIsActiveTrue(UUID productId);
}
