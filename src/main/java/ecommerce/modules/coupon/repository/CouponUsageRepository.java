package ecommerce.modules.coupon.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.modules.coupon.entity.CouponUsage;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CouponUsageRepository extends BaseRepository<CouponUsage, UUID> {

    List<CouponUsage> findByUserId(UUID userId);

    List<CouponUsage> findByCouponId(UUID couponId);
}
