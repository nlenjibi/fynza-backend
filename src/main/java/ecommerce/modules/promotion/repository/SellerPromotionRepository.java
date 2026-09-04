package ecommerce.modules.promotion.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.promotion.entity.SellerPromotion;
import ecommerce.modules.promotion.entity.SellerPromotion.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface SellerPromotionRepository extends BaseRepository<SellerPromotion, UUID> {

    Page<SellerPromotion> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<SellerPromotion> findBySellerIdAndStatusOrderByCreatedAtDesc(UUID sellerId, PromotionStatus status, Pageable pageable);

    Page<SellerPromotion> findBySellerIdAndPromotionTypeOrderByCreatedAtDesc(UUID sellerId, PromotionType type, Pageable pageable);

    @Query("SELECT p FROM SellerPromotion p WHERE p.sellerId = :sellerId AND p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate >= :now")
    List<SellerPromotion> findActivePromotions(@Param("sellerId") UUID sellerId, @Param("now") LocalDateTime now);

    @Query("SELECT COUNT(p) FROM SellerPromotion p WHERE p.sellerId = :sellerId AND p.status = 'ACTIVE'")
    long countActivePromotions(@Param("sellerId") UUID sellerId);

    @Query("SELECT SUM(p.usageCount) FROM SellerPromotion p WHERE p.sellerId = :sellerId")
    Long getTotalUsageCount(@Param("sellerId") UUID sellerId);
}
