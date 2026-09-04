package ecommerce.modules.coupon.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.coupon.dto.CouponRequest;
import ecommerce.modules.coupon.dto.CouponResponse;
import ecommerce.modules.coupon.entity.Coupon;
import ecommerce.modules.coupon.mapper.CouponMapper;
import ecommerce.modules.coupon.repository.CouponRepository;
import ecommerce.modules.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;
    private final CouponMapper couponMapper;

    @Override
    public List<CouponResponse> findAll() {
        log.debug("Fetching all coupons");
        return couponRepository.findAll().stream()
                .map(couponMapper::toResponse)
                .toList();
    }

    @Override
    public CouponResponse findById(UUID id) {
        log.debug("Fetching coupon by ID: {}", id);
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));
        return couponMapper.toResponse(coupon);
    }

    @Override
    public CouponResponse findByCode(String code) {
        log.debug("Fetching coupon by code: {}", code);
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));
        return couponMapper.toResponse(coupon);
    }

    @Override
    @Transactional
    public CouponResponse create(CouponRequest request) {
        log.info("Creating new coupon: {}", request.getCode());

        if (couponRepository.findByCode(request.getCode()).isPresent()) {
            throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
        }

        Coupon coupon = couponMapper.toEntity(request);
        Coupon savedCoupon = couponRepository.save(coupon);
        log.info("Coupon created successfully with ID: {}", savedCoupon.getId());

        return couponMapper.toResponse(savedCoupon);
    }

    @Override
    @Transactional
    public CouponResponse update(UUID id, CouponRequest request) {
        log.info("Updating coupon with ID: {}", id);

        Coupon coupon = findCouponById(id);

        if (request.getCode() != null && !request.getCode().equals(coupon.getCode())) {
            if (couponRepository.findByCode(request.getCode()).isPresent()) {
                throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
            }
        }

        couponMapper.updateEntityFromRequest(request, coupon);

        Coupon updatedCoupon = couponRepository.save(coupon);
        log.info("Coupon updated successfully: {}", updatedCoupon.getId());

        return couponMapper.toResponse(updatedCoupon);
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        log.info("Deleting coupon with ID: {}", id);

        Coupon coupon = findCouponById(id);
        couponRepository.delete(coupon);
        log.info("Coupon deleted successfully: {}", id);
    }

    @Override
    public Coupon validate(String code, BigDecimal orderAmount) {
        log.debug("Validating coupon code: {}", code);

        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with code: " + code));

        LocalDateTime now = LocalDateTime.now();

        if (coupon.getStatus() != ecommerce.common.enums.CouponStatus.ACTIVE) {
            throw new IllegalStateException("Coupon is not active");
        }

        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidUntil())) {
            throw new IllegalStateException("Coupon is not valid for the current date");
        }

        if (coupon.getMaxUses() != null && coupon.getUsageCount() >= coupon.getMaxUses()) {
            throw new IllegalStateException("Coupon usage limit has been reached");
        }

        if (coupon.getMinOrderAmount() != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalStateException("Order amount does not meet minimum requirement: " + coupon.getMinOrderAmount());
        }

        log.debug("Coupon validation successful for code: {}", code);
        return coupon;
    }

    private Coupon findCouponById(UUID id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found with ID: " + id));
    }
}
