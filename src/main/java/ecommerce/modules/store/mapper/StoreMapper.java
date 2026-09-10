package ecommerce.modules.store.mapper;

import ecommerce.modules.store.dto.response.*;
import ecommerce.modules.store.entity.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StoreMapper {

    public StoreResponse toResponse(Store store) {
        return StoreResponse.builder()
                .id(store.getPublicId())
                .storeName(store.getStoreName())
                .slug(store.getSlug())
                .description(store.getDescription())
                .logoMediaId(store.getLogoMediaId())
                .bannerMediaId(store.getBannerMediaId())
                .status(store.getStatus())
                .visibility(store.getVisibility())
                .businessEmail(store.getBusinessEmail())
                .businessPhone(store.getBusinessPhone())
                .website(store.getWebsite())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    public StoreDetailResponse toDetailResponse(Store store, StoreSetting setting, List<StorePolicy> policies) {
        return StoreDetailResponse.builder()
                .id(store.getPublicId())
                .storeName(store.getStoreName())
                .slug(store.getSlug())
                .description(store.getDescription())
                .logoMediaId(store.getLogoMediaId())
                .bannerMediaId(store.getBannerMediaId())
                .status(store.getStatus())
                .visibility(store.getVisibility())
                .businessEmail(store.getBusinessEmail())
                .businessPhone(store.getBusinessPhone())
                .website(store.getWebsite())
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .settings(setting != null ? toSettingResponse(setting) : null)
                .policies(policies.stream().map(this::toPolicyResponse).toList())
                .build();
    }

    public StoreSummaryResponse toSummary(StoreSummaryView view) {
        return StoreSummaryResponse.builder()
                .id(view.getPublicId())
                .storeName(view.getStoreName())
                .slug(view.getSlug())
                .logoMediaId(view.getLogoMediaId())
                .status(view.getStatus())
                .visibility(view.getVisibility())
                .sellerDisplayName(view.getSellerDisplayName())
                .productCount(view.getProductCount())
                .avgRating(view.getAvgRating())
                .reviewCount(view.getReviewCount())
                .orderCount(view.getOrderCount())
                .createdAt(view.getCreatedAt())
                .build();
    }

    public StoreSettingResponse toSettingResponse(StoreSetting setting) {
        return StoreSettingResponse.builder()
                .id(setting.getPublicId())
                .currency(setting.getCurrency())
                .timezone(setting.getTimezone())
                .language(setting.getLanguage())
                .orderNotifications(setting.getOrderNotifications())
                .customerNotifications(setting.getCustomerNotifications())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    public StorePolicyResponse toPolicyResponse(StorePolicy policy) {
        return StorePolicyResponse.builder()
                .id(policy.getPublicId())
                .type(policy.getType())
                .title(policy.getTitle())
                .content(policy.getContent())
                .version(policy.getVersion())
                .status(policy.getStatus())
                .effectiveFrom(policy.getEffectiveFrom())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }

    public StoreStatusHistoryResponse toStatusHistoryResponse(StoreStatusHistory history) {
        return StoreStatusHistoryResponse.builder()
                .id(history.getPublicId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .reason(history.getReason())
                .expiresAt(history.getExpiresAt())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
