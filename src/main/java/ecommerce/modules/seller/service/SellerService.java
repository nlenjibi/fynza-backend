package ecommerce.modules.seller.service;

import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.seller.dto.*;
import ecommerce.modules.tag.dto.TagResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface SellerService {

    SellerDashboardResponse getDashboard(UUID sellerId);

    Page<OrderResponse> getSellerOrders(UUID sellerId, Pageable pageable);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request, UUID sellerId);

    SellerAnalyticsResponse getSalesAnalytics(UUID sellerId, int days);

    SellerAnalyticsDto getSellerAnalytics(UUID sellerId);

    StoreResponse getStore(UUID sellerId);

    StoreResponse updateStore(UUID sellerId, UpdateStoreRequest request);

    Page<ReviewResponse> getSellerReviews(UUID sellerId, Pageable pageable);

    List<TagResponse> getTags();

    void assignTagsToProduct(UUID productId, List<String> tagNames, UUID sellerId);

    SellerPaymentSettingsResponse getPaymentSettings(UUID sellerId);

    SellerPaymentSettingsResponse updatePaymentSettings(UUID sellerId, SellerPaymentSettingsRequest request);

    SellerShippingSettingsResponse getShippingSettings(UUID sellerId);

    SellerShippingSettingsResponse updateShippingSettings(UUID sellerId, SellerShippingSettingsRequest request);

    ShippingZoneResponse createShippingZone(UUID sellerId, ShippingZoneRequest request);

    ShippingZoneResponse updateShippingZone(UUID sellerId, UUID zoneId, ShippingZoneRequest request);

    void deleteShippingZone(UUID sellerId, UUID zoneId);

    List<ShippingZoneResponse> getShippingZones(UUID sellerId);

    SellerNotificationSettingsResponse getNotificationSettings(UUID sellerId);

    SellerNotificationSettingsResponse updateNotificationSettings(UUID sellerId, SellerNotificationSettingsRequest request);
}
