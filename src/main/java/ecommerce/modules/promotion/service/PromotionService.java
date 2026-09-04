package ecommerce.modules.promotion.service;

import ecommerce.modules.promotion.dto.PromotionResponse;

import java.util.List;
import java.util.UUID;

public interface PromotionService {

    List<PromotionResponse> getAvailablePromotions();

    List<PromotionResponse> getFeaturedPromotions();

    List<PromotionResponse> getSellerPromotions(UUID sellerId);

    PromotionResponse getPromotion(UUID promotionId);

    void joinPromotion(UUID promotionId, UUID sellerId);

    void leavePromotion(UUID promotionId, UUID sellerId);
}
