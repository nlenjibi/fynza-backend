package ecommerce.modules.promotion.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.activity.entity.AdminPromotionActivity;
import ecommerce.modules.activity.repository.AdminPromotionActivityRepository;
import ecommerce.modules.promotion.entity.AdminPromotion;
import ecommerce.modules.promotion.entity.AdminPromotion.*;
import ecommerce.modules.promotion.repository.AdminPromotionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminPromotionService {

    private final AdminPromotionRepository promotionRepository;
    private final AdminPromotionActivityRepository activityRepository;

    @Transactional
    public AdminPromotion createPromotion(UUID adminId, String name, PromotionType type, String code,
                                         BigDecimal discountValue, BigDecimal minPurchase, BigDecimal maxDiscount,
                                         LocalDateTime startDate, LocalDateTime endDate, Integer usageLimit,
                                         UUID categoryId, Boolean isGlobal, String ipAddress) {
        AdminPromotion promotion = AdminPromotion.builder()
                .name(name)
                .promotionType(type)
                .code(code)
                .discountValue(discountValue)
                .minPurchase(minPurchase)
                .maxDiscount(maxDiscount)
                .startDate(startDate)
                .endDate(endDate)
                .usageLimit(usageLimit)
                .targetCategoryId(categoryId)
                .isGlobal(isGlobal)
                .status(determineStatus(startDate, endDate))
                .build();
        
        promotion = promotionRepository.save(promotion);
        
        logActivity(adminId, promotion.getPublicId(), AdminPromotionActivity.ActivityType.PROMOTION_CREATED,
                name, code, discountValue, "Promotion created", ipAddress);
        
        return promotion;
    }

    @Transactional
    public void deletePromotion(UUID adminId, UUID promotionId, String ipAddress) {
        AdminPromotion promotion = promotionRepository.findByPublicId(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        
        promotion.setIsActive(false);
        promotionRepository.save(promotion);
        
        logActivity(adminId, promotionId, AdminPromotionActivity.ActivityType.PROMOTION_DELETED,
                promotion.getName(), promotion.getCode(), promotion.getDiscountValue(), "Promotion deleted", ipAddress);
    }

    @Transactional
    public void applyCoupon(UUID adminId, UUID promotionId, BigDecimal orderAmount, String ipAddress) {
        AdminPromotion promotion = promotionRepository.findByPublicId(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        
        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotion.setTotalRevenue(promotion.getTotalRevenue().add(orderAmount));
        promotionRepository.save(promotion);
        
        logActivity(adminId, promotionId, AdminPromotionActivity.ActivityType.COUPON_USED,
                promotion.getName(), promotion.getCode(), promotion.getDiscountValue(), "Coupon used", ipAddress);
    }

    @Transactional(readOnly = true)
    public Page<AdminPromotion> getPromotions(Pageable pageable) {
        return promotionRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<AdminPromotion> getActivePromotions() {
        return promotionRepository.findActivePromotions(LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countActivePromotions() {
        return promotionRepository.countActivePromotions();
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue() {
        return promotionRepository.getTotalRevenue();
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(UUID adminId, UUID promotionId, AdminPromotionActivity.ActivityType type,
                            String name, String code, BigDecimal value, String description, String ipAddress) {
        AdminPromotionActivity activity = AdminPromotionActivity.builder()
                .adminId(adminId)
                .promotionId(promotionId)
                .activityType(type)
                .promotionName(name)
                .promoCode(code)
                .discountValue(value)
                .description(description)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();
        activityRepository.save(activity);
        log.debug("Admin promotion activity logged: {} for promotion: {}", type, name);
    }

    private PromotionStatus determineStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) return PromotionStatus.SCHEDULED;
        if (now.isAfter(endDate)) return PromotionStatus.EXPIRED;
        return PromotionStatus.ACTIVE;
    }
}
