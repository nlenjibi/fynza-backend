package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.AdminPromotionActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdminPromotionActivityRepository extends JpaRepository<AdminPromotionActivity, UUID> {

    Page<AdminPromotionActivity> findByAdminIdOrderByCreatedAtDesc(UUID adminId, Pageable pageable);

    Page<AdminPromotionActivity> findByPromotionIdOrderByCreatedAtDesc(UUID promotionId, Pageable pageable);
}
