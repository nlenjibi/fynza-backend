package ecommerce.modules.promotion.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.promotion.dto.PromotionResponse;
import ecommerce.modules.promotion.entity.Promotion;
import ecommerce.modules.promotion.entity.PromotionSeller;
import ecommerce.modules.promotion.repository.PromotionRepository;
import ecommerce.modules.promotion.repository.PromotionSellerRepository;
import ecommerce.modules.promotion.service.PromotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PromotionServiceImpl implements PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionSellerRepository promotionSellerRepository;

    @Override
    public List<PromotionResponse> getAvailablePromotions() {
        LocalDateTime now = LocalDateTime.now();
        return promotionRepository.findActivePromotions(now).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getFeaturedPromotions() {
        return promotionRepository.findFeaturedPromotions().stream()
                .map(p -> mapToResponseWithParticipation(p, null))
                .collect(Collectors.toList());
    }

    @Override
    public List<PromotionResponse> getSellerPromotions(UUID sellerId) {
        return promotionRepository.findActivePromotions(LocalDateTime.now()).stream()
                .map(p -> mapToResponseWithParticipation(p, sellerId))
                .collect(Collectors.toList());
    }

    @Override
    public PromotionResponse getPromotion(UUID promotionId) {
        Promotion promotion = promotionRepository.findByPublicId(promotionId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Promotion", promotionId));
        return mapToResponse(promotion);
    }

    @Override
    @Transactional
    public void joinPromotion(UUID promotionId, UUID sellerId) {
        Promotion promotion = promotionRepository.findByPublicId(promotionId)
                .orElseThrow(() -> ResourceNotFoundException.forResource("Promotion", promotionId));

        if (!promotion.getIsActive()) {
            throw new BadRequestException("Promotion is not active");
        }

        if (promotionSellerRepository.existsByPromotionIdAndSellerId(promotionId, sellerId)) {
            throw new BadRequestException("You have already joined this promotion");
        }

        PromotionSeller participation = PromotionSeller.builder()
                .promotion(promotion)
                .sellerId(sellerId)
                .joinedAt(LocalDateTime.now())
                .build();

        promotionSellerRepository.save(participation);
        log.info("Seller {} joined promotion {}", sellerId, promotionId);
    }

    @Override
    @Transactional
    public void leavePromotion(UUID promotionId, UUID sellerId) {
        PromotionSeller participation = promotionSellerRepository
                .findByPromotionIdAndSellerId(promotionId, sellerId)
                .orElseThrow(() -> new BadRequestException("You are not participating in this promotion"));

        promotionSellerRepository.delete(participation);
        log.info("Seller {} left promotion {}", sellerId, promotionId);
    }

    private PromotionResponse mapToResponse(Promotion promotion) {
        return PromotionResponse.builder()
                .id(promotion.getPublicId())
                .name(promotion.getName())
                .description(promotion.getDescription())
                .bannerImage(promotion.getBannerImage())
                .discountType(promotion.getDiscountType())
                .discountValue(promotion.getDiscountValue())
                .minOrderAmount(promotion.getMinOrderAmount())
                .maxDiscountAmount(promotion.getMaxDiscountAmount())
                .startDate(promotion.getStartDate())
                .endDate(promotion.getEndDate())
                .maxUses(promotion.getMaxUses())
                .currentUses(promotion.getCurrentUses())
                .maxUsesPerUser(promotion.getMaxUsesPerUser())
                .isExclusive(promotion.getIsExclusive())
                .isFeatured(promotion.getIsFeatured())
                .termsConditions(promotion.getTermsConditions())
                .status(promotion.getStatus().name())
                .build();
    }

    private PromotionResponse mapToResponseWithParticipation(Promotion promotion, UUID sellerId) {
        PromotionResponse response = mapToResponse(promotion);
        if (sellerId != null) {
            response.setIsParticipating(
                    promotionSellerRepository.existsByPromotionIdAndSellerId(promotion.getPublicId(), sellerId));
        }
        return response;
    }
}
