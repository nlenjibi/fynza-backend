package ecommerce.modules.promotion.service;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.activity.entity.SellerPromotionActivity;
import ecommerce.modules.activity.repository.SellerPromotionActivityRepository;
import ecommerce.modules.promotion.dto.SellerPromotionDto;
import ecommerce.modules.promotion.entity.SellerPromotion;
import ecommerce.modules.promotion.repository.SellerPromotionRepository;
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
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SellerPromotionService {

    private final SellerPromotionRepository promotionRepository;
    private final SellerPromotionActivityRepository activityRepository;

    @Transactional
    public SellerPromotionDto createPromotion(UUID sellerId, String name, String type,
                                         BigDecimal discountValue, BigDecimal minPurchase,
                                         LocalDateTime startDate, LocalDateTime endDate,
                                         Integer usageLimit, String ipAddress) {
        SellerPromotion promotion = SellerPromotion.builder()
                .sellerId(sellerId)
                .name(name)
                .promotionType(SellerPromotion.PromotionType.valueOf(type.toUpperCase()))
                .discountValue(discountValue)
                .minPurchase(minPurchase)
                .startDate(startDate)
                .endDate(endDate)
                .usageLimit(usageLimit)
                .status(determineStatus(startDate, endDate))
                .build();
        
        promotion = promotionRepository.save(promotion);
        
        logActivity(sellerId, promotion.getPublicId(), SellerPromotionActivity.ActivityType.PROMOTION_CREATED,
                name, discountValue, "Promotion created", ipAddress);
        
        return mapToDto(promotion);
    }

    @Transactional
    public void deletePromotion(UUID sellerId, UUID promotionId, String ipAddress) {
        SellerPromotion promotion = promotionRepository.findByPublicId(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        
        promotion.setIsActive(false);
        promotionRepository.save(promotion);
        
        logActivity(sellerId, promotionId, SellerPromotionActivity.ActivityType.PROMOTION_DELETED,
                promotion.getName(), promotion.getDiscountValue(), "Promotion deleted", ipAddress);
    }

    @Transactional
    public void applyDiscount(UUID sellerId, UUID promotionId, BigDecimal orderAmount, String ipAddress) {
        SellerPromotion promotion = promotionRepository.findByPublicId(promotionId)
                .orElseThrow(() -> new ResourceNotFoundException("Promotion not found"));
        
        promotion.setUsageCount(promotion.getUsageCount() + 1);
        promotionRepository.save(promotion);
        
        logActivity(sellerId, promotionId, SellerPromotionActivity.ActivityType.DISCOUNT_APPLIED,
                promotion.getName(), promotion.getDiscountValue(), "Discount applied to order", ipAddress);
    }

    @Transactional(readOnly = true)
    public Page<SellerPromotionDto> getPromotions(UUID sellerId, Pageable pageable) {
        return promotionRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public List<SellerPromotionDto> getActivePromotions(UUID sellerId) {
        return promotionRepository.findActivePromotions(sellerId, LocalDateTime.now()).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logActivity(UUID sellerId, UUID promotionId, SellerPromotionActivity.ActivityType type,
                            String name, BigDecimal value, String description, String ipAddress) {
        SellerPromotionActivity activity = SellerPromotionActivity.builder()
                .sellerId(sellerId)
                .promotionId(promotionId)
                .activityType(type)
                .promotionName(name)
                .discountValue(value)
                .description(description)
                .ipAddress(ipAddress)
                .createdAt(Instant.now())
                .build();
        activityRepository.save(activity);
        log.debug("Seller promotion activity logged: {} for promotion: {}", type, name);
    }

    private SellerPromotion.PromotionStatus determineStatus(LocalDateTime startDate, LocalDateTime endDate) {
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(startDate)) return SellerPromotion.PromotionStatus.SCHEDULED;
        if (now.isAfter(endDate)) return SellerPromotion.PromotionStatus.EXPIRED;
        return SellerPromotion.PromotionStatus.ACTIVE;
    }

    private SellerPromotionDto mapToDto(SellerPromotion promotion) {
        return SellerPromotionDto.builder()
                .id(promotion.getPublicId())
                .sellerId(promotion.getSellerId())
                .name(promotion.getName())
                .promotionType(promotion.getPromotionType().name())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .minPurchase(promotion.getMinPurchase())
                .maxDiscount(promotion.getMaxDiscount())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .usageLimit(promotion.getUsageLimit())
                .usageCount(promotion.getUsageCount())
                .status(promotion.getStatus().name())
                .isActive(promotion.getIsActive())
                .createdAt(promotion.getCreatedAt() != null ? promotion.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDateTime() : null)
                .build();
    }
}
