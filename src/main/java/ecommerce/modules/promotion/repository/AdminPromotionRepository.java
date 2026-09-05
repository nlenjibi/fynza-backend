package ecommerce.modules.promotion.repository;

import ecommerce.modules.promotion.entity.AdminPromotion;
import ecommerce.modules.promotion.entity.AdminPromotion.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminPromotionRepository extends JpaRepository<AdminPromotion, Long> {

    Optional<AdminPromotion> findByPublicId(UUID publicId);

    Page<AdminPromotion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<AdminPromotion> findByStatusOrderByCreatedAtDesc(PromotionStatus status, Pageable pageable);

    @Query("SELECT p FROM AdminPromotion p WHERE p.status = 'ACTIVE' AND p.startDate <= :now AND p.endDate >= :now")
    List<AdminPromotion> findActivePromotions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(p) FROM AdminPromotion p WHERE p.status = 'ACTIVE'")
    long countActivePromotions();

    @Query("SELECT SUM(p.totalRevenue) FROM AdminPromotion p")
    BigDecimal getTotalRevenue();

    @Query("SELECT SUM(p.usageCount) FROM AdminPromotion p")
    Long getTotalUsageCount();
}
