package ecommerce.modules.coupon.repository;

import ecommerce.common.base.BaseRepository;
import ecommerce.common.enums.CouponStatus;
import ecommerce.modules.coupon.entity.Coupon;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends BaseRepository<Coupon, UUID> {

    Optional<Coupon> findByCode(String code);

    java.util.List<Coupon> findByStatus(CouponStatus status);
}
