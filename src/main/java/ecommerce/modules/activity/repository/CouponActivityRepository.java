package ecommerce.modules.activity.repository;

import ecommerce.modules.activity.entity.CouponActivity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CouponActivityRepository extends JpaRepository<CouponActivity, UUID> {

    Page<CouponActivity> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<CouponActivity> findBySellerIdOrderByCreatedAtDesc(UUID sellerId, Pageable pageable);

    Page<CouponActivity> findByCouponIdOrderByCreatedAtDesc(UUID couponId, Pageable pageable);
}
