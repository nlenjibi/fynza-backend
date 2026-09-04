package ecommerce.modules.coupon.service;

import ecommerce.modules.coupon.dto.CouponRequest;
import ecommerce.modules.coupon.dto.CouponResponse;
import ecommerce.modules.coupon.entity.Coupon;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CouponService {

    List<CouponResponse> findAll();

    CouponResponse findById(UUID id);

    CouponResponse findByCode(String code);

    CouponResponse create(CouponRequest request);

    CouponResponse update(UUID id, CouponRequest request);

    void delete(UUID id);

    Coupon validate(String code, BigDecimal orderAmount);
}
