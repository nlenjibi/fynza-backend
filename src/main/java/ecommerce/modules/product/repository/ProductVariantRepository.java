package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, UUID> {

    List<ProductVariant> findByProductIdAndIsActiveTrueOrderBySku(UUID productId);

    boolean existsByProductIdAndSku(UUID productId, String sku);

    Optional<ProductVariant> findByIdAndProductId(UUID id, UUID productId);

    long countByProductIdAndIsActiveTrue(UUID productId);
}
