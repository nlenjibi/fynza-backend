package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductAttribute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductAttributeRepository extends JpaRepository<ProductAttribute, Long> {

    List<ProductAttribute> findByProductIdOrderBySortOrder(UUID productId);

    void deleteByProductId(UUID productId);
}
