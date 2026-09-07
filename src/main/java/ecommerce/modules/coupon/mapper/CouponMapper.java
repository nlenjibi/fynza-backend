package ecommerce.modules.coupon.mapper;

import ecommerce.modules.coupon.dto.CouponRequest;
import ecommerce.modules.coupon.dto.CouponResponse;
import ecommerce.modules.coupon.entity.Coupon;

public interface CouponMapper {
    CouponResponse toResponse(Coupon coupon);
    Coupon toEntity(CouponRequest request);
}
