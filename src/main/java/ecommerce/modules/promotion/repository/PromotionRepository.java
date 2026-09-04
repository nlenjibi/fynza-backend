package ecommerce.modules.promotion.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.promotion.entity.Promotion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PromotionRepository extends BaseRepository<Promotion, UUID> {

    Page<Promotion> findByIsActiveTrue(Pageable pageable);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.startDate <= :now AND p.endDate >= :now")
    List<Promotion> findActivePromotions(@Param("now") LocalDateTime now);

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.isFeatured = true")
    List<Promotion> findFeaturedPromotions();

    @Query("SELECT p FROM Promotion p WHERE p.isActive = true AND p.endDate < :now")
    List<Promotion> findExpiredPromotions(@Param("now") LocalDateTime now);
}
