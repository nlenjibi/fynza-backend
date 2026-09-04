package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    List<ProductVariant> findByProductId(Long productId);

    @Query("SELECT v FROM ProductVariant v WHERE v.product.id IN :productIds")
    List<ProductVariant> findByProductIdIn(java.util.Collection<Long> productIds);

    Optional<ProductVariant> findBySku(String sku);
}
