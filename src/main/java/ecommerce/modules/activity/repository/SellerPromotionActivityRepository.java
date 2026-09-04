package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.SellerPromotionActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SellerPromotionActivityRepository extends JpaRepository<SellerPromotionActivity, UUID> {

    Page<SellerPromotionActivity> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<SellerPromotionActivity> findByPromotionIdOrderByCreatedAtDesc(UUID promotionId, Pageable pageable);
}
