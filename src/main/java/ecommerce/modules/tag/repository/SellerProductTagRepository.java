package ecommerce.modules.tag.repository;

import ecommerce.modules.tag.entity.SellerProductTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SellerProductTagRepository extends JpaRepository<SellerProductTag, UUID> {

    List<SellerProductTag> findBySellerId(UUID sellerId);

    List<SellerProductTag> findByProductId(UUID productId);

    List<SellerProductTag> findBySellerIdAndProductId(UUID sellerId, UUID productId);

    Optional<SellerProductTag> findByProductIdAndTagId(UUID productId, UUID tagId);

    @Query("SELECT DISTINCT spt.productId FROM SellerProductTag spt WHERE spt.sellerId = :sellerId")
    List<UUID> findProductIdsBySellerId(@Param("sellerId") UUID sellerId);

    @Query("SELECT DISTINCT spt.tagId FROM SellerProductTag spt WHERE spt.sellerId = :sellerId")
    List<UUID> findTagIdsBySellerId(@Param("sellerId") UUID sellerId);

    @Query("SELECT spt.tagId, COUNT(spt) FROM SellerProductTag spt WHERE spt.sellerId = :sellerId GROUP BY spt.tagId ORDER BY COUNT(spt) DESC")
    List<Object[]> countByTagGroupedBySeller(@Param("sellerId") UUID sellerId);

    void deleteByProductIdAndTagId(UUID productId, UUID tagId);

    @Query("DELETE FROM SellerProductTag spt WHERE spt.productId = :productId")
    void deleteByProductId(@Param("productId") UUID productId);
}
