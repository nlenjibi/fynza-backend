package ecommerce.modules.pricing.mapper;

import ecommerce.modules.pricing.dto.response.*;
import ecommerce.modules.pricing.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PriceMapper {

    public PriceResponse toResponse(Price price) {
        return PriceResponse.builder()
                .publicId(price.getPublicId())
                .productId(price.getProductId())
                .variantId(price.getVariantId())
                .amount(price.getAmount())
                .saleAmount(price.getSaleAmount())
                .currency(price.getCurrency().name())
                .status(price.getStatus().name())
                .validFrom(price.getValidFrom())
                .validUntil(price.getValidUntil())
                .isActive(price.getIsActive())
                .tiers(mapTiers(price.getTiers()))
                .createdAt(price.getCreatedAt())
                .updatedAt(price.getUpdatedAt())
                .build();
    }

    public PriceTierResponse toTierResponse(PriceTier tier) {
        return PriceTierResponse.builder()
                .publicId(tier.getPublicId())
                .minQuantity(tier.getMinQuantity())
                .maxQuantity(tier.getMaxQuantity())
                .unitPrice(tier.getUnitPrice())
                .currency(tier.getCurrency().name())
                .isActive(tier.getIsActive())
                .createdAt(tier.getCreatedAt())
                .build();
    }

    public PriceHistoryResponse toHistoryResponse(PriceHistory history) {
        return PriceHistoryResponse.builder()
                .id(history.getId())
                .oldAmount(history.getOldAmount())
                .newAmount(history.getNewAmount())
                .oldCurrency(history.getOldCurrency())
                .newCurrency(history.getNewCurrency())
                .oldStatus(history.getOldStatus())
                .newStatus(history.getNewStatus())
                .changedBy(history.getChangedBy())
                .reason(history.getReason())
                .createdAt(history.getCreatedAt())
                .build();
    }

    public PriceListResponse toPriceListResponse(PriceList priceList) {
        return PriceListResponse.builder()
                .publicId(priceList.getPublicId())
                .name(priceList.getName())
                .description(priceList.getDescription())
                .isDefault(priceList.getIsDefault())
                .isActive(priceList.getIsActive())
                .createdAt(priceList.getCreatedAt())
                .build();
    }

    private List<PriceTierResponse> mapTiers(List<PriceTier> tiers) {
        if (tiers == null) return List.of();
        return tiers.stream()
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .map(this::toTierResponse)
                .toList();
    }
}
