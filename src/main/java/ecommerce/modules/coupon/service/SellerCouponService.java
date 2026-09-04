package ecommerce.modules.coupon.service;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.coupon.entity.SellerCoupon;
import ecommerce.modules.coupon.entity.SellerCoupon.DiscountType;
import ecommerce.modules.coupon.entity.SellerCoupon.Status;
import ecommerce.modules.coupon.repository.SellerCouponRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerCouponService {

    private final SellerCouponRepository sellerCouponRepository;

    @Transactional
    public SellerCoupon createCoupon(UUID sellerId, String code, String name, String description,
                                   DiscountType discountType, BigDecimal discountValue,
                                   BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                   Integer maxUses, Integer maxUsesPerUser,
                                   LocalDateTime validFrom, LocalDateTime validUntil) {
        if (sellerCouponRepository.findByCodeIgnoreCase(code, sellerId).isPresent()) {
            throw new BadRequestException("Coupon code already exists");
        }

        SellerCoupon coupon = SellerCoupon.builder()
                .sellerId(sellerId)
                .code(code.toUpperCase())
                .name(name)
                .description(description)
                .discountType(discountType)
                .discountValue(discountValue)
                .minOrderAmount(minOrderAmount)
                .maxDiscountAmount(maxDiscountAmount)
                .maxUses(maxUses)
                .maxUsesPerUser(maxUsesPerUser)
                .validFrom(validFrom)
                .validUntil(validUntil)
                .status(determineStatus(validFrom, validUntil))
                .build();

        log.info("Creating coupon: {} for seller: {}", code, sellerId);
        return sellerCouponRepository.save(coupon);
    }

    @Transactional
    public SellerCoupon updateCoupon(UUID id, UUID sellerId, String name, String description,
                                    DiscountType discountType, BigDecimal discountValue,
                                    BigDecimal minOrderAmount, BigDecimal maxDiscountAmount,
                                    LocalDateTime validFrom, LocalDateTime validUntil) {
        SellerCoupon coupon = sellerCouponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (!coupon.getSellerId().equals(sellerId)) {
            throw new BadRequestException("Not authorized to update this coupon");
        }

        if (name != null) coupon.setName(name);
        if (description != null) coupon.setDescription(description);
        if (discountType != null) coupon.setDiscountType(discountType);
        if (discountValue != null) coupon.setDiscountValue(discountValue);
        if (minOrderAmount != null) coupon.setMinOrderAmount(minOrderAmount);
        if (maxDiscountAmount != null) coupon.setMaxDiscountAmount(maxDiscountAmount);
        if (validFrom != null) coupon.setValidFrom(validFrom);
        if (validUntil != null) coupon.setValidUntil(validUntil);

        coupon.setStatus(determineStatus(coupon.getValidFrom(), coupon.getValidUntil()));

        log.info("Updating coupon: {}", id);
        return sellerCouponRepository.save(coupon);
    }

    @Transactional
    public void deleteCoupon(UUID id, UUID sellerId) {
        SellerCoupon coupon = sellerCouponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (!coupon.getSellerId().equals(sellerId)) {
            throw new BadRequestException("Not authorized to delete this coupon");
        }

        coupon.setIsActive(false);
        sellerCouponRepository.save(coupon);
        log.info("Deleted coupon: {}", id);
    }

    @Transactional
    public SellerCoupon pauseCoupon(UUID id, UUID sellerId) {
        SellerCoupon coupon = sellerCouponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (!coupon.getSellerId().equals(sellerId)) {
            throw new BadRequestException("Not authorized to pause this coupon");
        }

        coupon.setStatus(Status.PAUSED);
        log.info("Paused coupon: {}", id);
        return sellerCouponRepository.save(coupon);
    }

    @Transactional
    public SellerCoupon activateCoupon(UUID id, UUID sellerId) {
        SellerCoupon coupon = sellerCouponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (!coupon.getSellerId().equals(sellerId)) {
            throw new BadRequestException("Not authorized to activate this coupon");
        }

        coupon.setStatus(Status.ACTIVE);
        log.info("Activated coupon: {}", id);
        return sellerCouponRepository.save(coupon);
    }

    @Transactional(readOnly = true)
    public Page<SellerCoupon> getSellerCoupons(UUID sellerId, Pageable pageable) {
        return sellerCouponRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SellerCoupon> getSellerCouponsByStatus(UUID sellerId, Status status, Pageable pageable) {
        return sellerCouponRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(sellerId, status, pageable);
    }

    @Transactional(readOnly = true)
    public SellerCoupon getCoupon(UUID id) {
        return sellerCouponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
    }

    @Transactional(readOnly = true)
    public SellerCoupon validateCoupon(String code, UUID sellerId) {
        return sellerCouponRepository.findByCodeAndSellerId(code.toUpperCase(), sellerId)
                .orElseThrow(() -> new BadRequestException("Coupon not found"));
    }

    @Transactional(readOnly = true)
    public boolean isValidCoupon(String code, UUID sellerId) {
        return sellerCouponRepository.findValidCoupons(sellerId, LocalDateTime.now())
                .stream()
                .anyMatch(c -> c.getCode().equalsIgnoreCase(code));
    }

    private Status determineStatus(LocalDateTime validFrom, LocalDateTime validUntil) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(validFrom)) return Status.PAUSED;
        if (now.isAfter(validUntil)) return Status.EXPIRED;
        return Status.ACTIVE;
    }
}
