package ecommerce.modules.coupon.repository;

import ecommerce.common.enums.CouponStatus;
import ecommerce.modules.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByPublicId(UUID publicId);

    Optional<Coupon> findByCode(String code);

    java.util.List<Coupon> findByStatus(CouponStatus status);
}
