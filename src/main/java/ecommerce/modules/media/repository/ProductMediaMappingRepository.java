package ecommerce.modules.media.repository;

import ecommerce.modules.media.entity.ProductMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductMediaMappingRepository extends JpaRepository<ProductMedia, Long> {

    List<ProductMedia> findByProductIdOrderBySortOrderAsc(UUID productId);

    Optional<ProductMedia> findByProductIdAndMediaAssetId(UUID productId, Long mediaAssetId);

    Optional<ProductMedia> findByProductIdAndIsPrimaryTrue(UUID productId);

    @Modifying
    @Query("UPDATE ProductMedia pm SET pm.isPrimary = false WHERE pm.productId = :productId")
    void clearPrimaryForProduct(@Param("productId") UUID productId);
}
