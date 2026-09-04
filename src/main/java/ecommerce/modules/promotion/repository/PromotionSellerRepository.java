package ecommerce.modules.promotion.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.promotion.entity.Promotion;
import ecommerce.modules.promotion.entity.PromotionSeller;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromotionSellerRepository extends BaseRepository<PromotionSeller, UUID> {

    List<PromotionSeller> findBySellerId(UUID sellerId);

    Optional<PromotionSeller> findByPromotionIdAndSellerId(UUID promotionId, UUID sellerId);

    boolean existsByPromotionIdAndSellerId(UUID promotionId, UUID sellerId);

    @Query("SELECT ps.promotion FROM PromotionSeller ps WHERE ps.sellerId = :sellerId")
    List<Promotion> findPromotionsBySellerId(@Param("sellerId") UUID sellerId);
}
