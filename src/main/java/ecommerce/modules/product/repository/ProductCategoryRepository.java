package ecommerce.modules.product.repository;

import ecommerce.modules.product.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByProductIdOrderByIsPrimaryDesc(UUID productId);

    Optional<ProductCategory> findByProductIdAndCategoryId(UUID productId, UUID categoryId);

    Optional<ProductCategory> findByProductIdAndIsPrimaryTrue(UUID productId);

    boolean existsByProductIdAndCategoryId(UUID productId, UUID categoryId);

    void deleteByProductIdAndCategoryId(UUID productId, UUID categoryId);
}
