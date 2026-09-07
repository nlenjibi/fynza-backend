package ecommerce.modules.coupon.repository;

import ecommerce.modules.coupon.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {

    List<CouponUsage> findByUserId(UUID userId);

    List<CouponUsage> findByCouponId(Long couponId);
}
