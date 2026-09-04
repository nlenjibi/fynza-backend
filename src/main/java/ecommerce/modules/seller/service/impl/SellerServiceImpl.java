package ecommerce.modules.seller.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.notification.entity.SellerNotificationSettings;
import ecommerce.modules.notification.repository.SellerNotificationSettingsRepository;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.order.dto.SellerOrderDto;
import ecommerce.modules.order.mapper.OrderMapper;
import ecommerce.modules.order.repository.OrderItemRepository;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.entity.Product;
import ecommerce.modules.product.repository.ProductRepository;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.entity.Review;
import ecommerce.modules.review.repository.ReviewRepository;
import ecommerce.modules.seller.dto.*;
import ecommerce.modules.seller.entity.ShippingZone;
import ecommerce.modules.seller.repository.ShippingZoneRepository;
import ecommerce.modules.seller.service.SellerService;
import ecommerce.modules.tag.dto.TagResponse;
import ecommerce.modules.tag.service.TagService;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.repository.SellerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Seller service implementation.
 * Handles seller dashboard and analytics.
 * 
 * Note: Order-related methods delegate to OrderService for consistency.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SellerServiceImpl implements SellerService {

    private final OrderService orderService;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;
    private final SellerProfileRepository sellerProfileRepository;
    private final ReviewRepository reviewRepository;
    private final TagService tagService;
    private final ShippingZoneRepository shippingZoneRepository;
    private final SellerNotificationSettingsRepository sellerNotificationSettingsRepository;

    @Override
    public SellerDashboardResponse getDashboard(UUID sellerId) {
        log.info("Getting seller dashboard for: {}", sellerId);
        
        var products = productRepository.findBySellerId(sellerId, Pageable.unpaged()).getContent();
        
        long totalProducts = products.size();
        long activeProducts = products.stream().filter(p -> p.getIsActive()).count();
        
        Double averageRating = products.stream()
                .filter(p -> p.getRating() != null)
                .mapToDouble(p -> p.getRating().doubleValue())
                .average()
                .orElse(0.0);
        
        long storeVisits = products.stream()
                .mapToLong(p -> p.getViewCount() != null ? p.getViewCount().longValue() : 0L)
                .sum();
        
        long lastMonthVisits = storeVisits > 0 ? (long)(storeVisits * 0.9) : 0;
        double visitGrowth = lastMonthVisits > 0 ? (double)(storeVisits - lastMonthVisits) / lastMonthVisits * 100 : 0;

        SellerOrderDto orderDashboard = orderService.getSellerOrderDashboard(sellerId);

        return SellerDashboardResponse.builder()
                .totalProducts(totalProducts)
                .activeProducts(activeProducts)
                .totalOrders(orderDashboard.getTotalOrders())
                .ordersThisMonth(orderDashboard.getOrdersThisMonth())
                .pendingOrders(orderDashboard.getPendingOrders())
                .completedOrders(orderDashboard.getCompletedOrders())
                .totalRevenue(orderDashboard.getTotalRevenue())
                .monthlyRevenue(orderDashboard.getMonthlyRevenue())
                .revenueGrowth(orderDashboard.getRevenueGrowth())
                .averageRating(averageRating)
                .totalCustomers(orderDashboard.getTotalCustomers())
                .storeVisits(storeVisits)
                .visitGrowth(visitGrowth)
                .recentOrders(convertRecentOrders(orderDashboard.getRecentOrders()))
                .topProducts(getTopProductsForSeller(sellerId, 5))
                .build();
    }

    @Override
    public Page<OrderResponse> getSellerOrders(UUID sellerId, Pageable pageable) {
        return orderService.getSellerOrders(sellerId, pageable);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request, UUID sellerId) {
        return orderService.updateSellerOrderStatus(orderId, request, sellerId);
    }

    @Override
    public SellerAnalyticsResponse getSalesAnalytics(UUID sellerId, int days) {
        SellerOrderDto.SellerOrderAnalytics analytics = orderService.getSellerOrderAnalytics(sellerId, days);

        long totalViews = productRepository.findBySellerId(sellerId, Pageable.unpaged()).getContent().stream()
                .mapToLong(p -> p.getViewCount() != null ? p.getViewCount().longValue() : 0L)
                .sum();
        double conversionRate = totalViews > 0 ? (double) analytics.getTotalOrders() / totalViews * 100 : 0.0;

        return SellerAnalyticsResponse.builder()
                .totalSales(analytics.getTotalSales())
                .averageOrderValue(analytics.getAverageOrderValue())
                .totalOrders(analytics.getTotalOrders())
                .totalProductsSold(analytics.getTotalProductsSold())
                .conversionRate(conversionRate)
                .dailySales(convertDailySales(analytics.getDailySales()))
                .topProducts(analytics.getTopProducts().stream()
                        .map(p -> SellerAnalyticsResponse.TopProduct.builder()
                                .productId(p.getProductId())
                                .productName(p.getProductName())
                                .quantitySold(p.getQuantitySold())
                                .revenue(p.getRevenue())
                                .build())
                        .toList())
                .build();
    }

    @Override
    public SellerAnalyticsDto getSellerAnalytics(UUID sellerId) {
        return orderService.getSellerAnalytics(sellerId);
    }

    private java.util.List<SellerDashboardResponse.RecentOrderDto> convertRecentOrders(
            java.util.List<SellerOrderDto.RecentSellerOrderDto> recentOrders) {
        if (recentOrders == null) return java.util.Collections.emptyList();
        return recentOrders.stream()
                .map(order -> SellerDashboardResponse.RecentOrderDto.builder()
                        .orderId(order.getOrderId())
                        .orderNumber(order.getOrderNumber())
                        .customerName(order.getCustomerName())
                        .productName(order.getProductName())
                        .amount(order.getAmount())
                        .status(order.getStatus())
                        .timeAgo(order.getTimeAgo())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private java.util.List<SellerAnalyticsResponse.DailySales> convertDailySales(
            java.util.List<SellerOrderDto.DailySalesDto> dailySales) {
        if (dailySales == null) return java.util.Collections.emptyList();
        return dailySales.stream()
                .map(sale -> SellerAnalyticsResponse.DailySales.builder()
                        .date(sale.getDate())
                        .sales(sale.getSales())
                        .orders(sale.getOrders())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    private java.util.List<SellerDashboardResponse.TopProductDto> getTopProductsForSeller(UUID sellerId, int limit) {
        return java.util.Collections.emptyList();
    }

    @Override
    public StoreResponse getStore(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return mapToStoreResponse(profile);
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID sellerId, UpdateStoreRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        if (request.getStoreName() != null) profile.setStoreName(request.getStoreName());
        if (request.getStoreDescription() != null) profile.setStoreDescription(request.getStoreDescription());
        if (request.getStoreWebsite() != null) profile.setStoreWebsite(request.getStoreWebsite());
        if (request.getStoreLogo() != null) profile.setStoreLogo(request.getStoreLogo());
        if (request.getStoreBanner() != null) profile.setStoreBanner(request.getStoreBanner());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getPhone() != null) profile.setPhone(request.getPhone());
        if (request.getRegion() != null) profile.setRegion(request.getRegion());
        if (request.getCity() != null) profile.setCity(request.getCity());
        if (request.getBusinessAddress() != null) profile.setBusinessAddress(request.getBusinessAddress());
        if (request.getWorkingHours() != null) profile.setWorkingHours(request.getWorkingHours());
        if (request.getFacebookUrl() != null) profile.setFacebookUrl(request.getFacebookUrl());
        if (request.getInstagramUrl() != null) profile.setInstagramUrl(request.getInstagramUrl());
        if (request.getTwitterUrl() != null) profile.setTwitterUrl(request.getTwitterUrl());
        if (request.getBusinessRegistration() != null) profile.setBusinessRegistration(request.getBusinessRegistration());
        if (request.getBankName() != null) profile.setBankName(request.getBankName());
        if (request.getAccountHolderName() != null) profile.setAccountHolderName(request.getAccountHolderName());
        if (request.getAccountNumber() != null) profile.setAccountNumber(request.getAccountNumber());

        SellerProfile saved = sellerProfileRepository.save(profile);
        return mapToStoreResponse(saved);
    }

    @Override
    public Page<ReviewResponse> getSellerReviews(UUID sellerId, Pageable pageable) {
        List<UUID> productIds = productRepository.findBySellerId(sellerId, Pageable.unpaged()).getContent()
                .stream()
                .map(p -> p.getId())
                .collect(Collectors.toList());

        if (productIds.isEmpty()) {
            return Page.empty(pageable);
        }

        List<ReviewResponse> reviews = reviewRepository.findAll().stream()
                .filter(r -> productIds.contains(r.getProduct().getId()))
                .map(this::mapToReviewResponse)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), reviews.size());
        List<ReviewResponse> pageContent = start < reviews.size() 
                ? reviews.subList(start, end) 
                : java.util.Collections.emptyList();

        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, reviews.size());
    }

    @Override
    public List<TagResponse> getTags() {
        return tagService.getActiveTags();
    }

    @Override
    @Transactional
    public void assignTagsToProduct(UUID productId, List<String> tagNames, UUID sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (!product.getSeller().getId().equals(sellerId)) {
            throw new ResourceNotFoundException("Product does not belong to this seller");
        }

        for (String tagName : tagNames) {
            TagResponse tag = tagService.getOrCreateTag(tagName);
            tagService.incrementUsage(tag.getId());
        }
    }

    private StoreResponse mapToStoreResponse(SellerProfile profile) {
        return StoreResponse.builder()
                .id(profile.getId())
                .storeName(profile.getStoreName())
                .storeDescription(profile.getStoreDescription())
                .storeWebsite(profile.getStoreWebsite())
                .storeLogo(profile.getStoreLogo())
                .storeBanner(profile.getStoreBanner())
                .email(profile.getEmail())
                .phone(profile.getPhone())
                .region(profile.getRegion())
                .city(profile.getCity())
                .businessAddress(profile.getBusinessAddress())
                .workingHours(profile.getWorkingHours())
                .facebookUrl(profile.getFacebookUrl())
                .instagramUrl(profile.getInstagramUrl())
                .twitterUrl(profile.getTwitterUrl())
                .rating(profile.getRating())
                .totalReviews(profile.getTotalReviews())
                .totalProducts(profile.getTotalProducts())
                .totalSales(profile.getTotalSales())
                .totalRevenue(profile.getTotalRevenue())
                .verificationStatus(profile.getVerificationStatus().name())
                .businessRegistration(profile.getBusinessRegistration())
                .bankName(profile.getBankName())
                .build();
    }

    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .productName(review.getProduct().getName())
                .user(ReviewResponse.UserInfo.builder()
                        .id(review.getCustomer().getId())
                        .email(review.getCustomer().getEmail())
                        .build())
                .rating(review.getRating())
                .title(review.getTitle())
                .comment(review.getComment())
                .verifiedPurchase(review.getVerifiedPurchase())
                .approved(review.getApproved())
                .helpfulCount(review.getHelpful())
                .createdAt(review.getCreatedAt())
                .build();
    }

    @Override
    public SellerPaymentSettingsResponse getPaymentSettings(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));
        return SellerPaymentSettingsResponse.builder()
                .id(profile.getId())
                .bankName(profile.getBankName())
                .accountHolderName(profile.getAccountHolderName())
                .accountNumber(maskAccountNumber(profile.getAccountNumber()))
                .branch(profile.getBranch())
                .payoutSchedule(profile.getPayoutSchedule())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public SellerPaymentSettingsResponse updatePaymentSettings(UUID sellerId, SellerPaymentSettingsRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        if (request.getBankName() != null) profile.setBankName(request.getBankName());
        if (request.getAccountHolderName() != null) profile.setAccountHolderName(request.getAccountHolderName());
        if (request.getAccountNumber() != null) profile.setAccountNumber(request.getAccountNumber());
        if (request.getBranch() != null) profile.setBranch(request.getBranch());
        if (request.getPayoutSchedule() != null) profile.setPayoutSchedule(request.getPayoutSchedule());

        SellerProfile saved = sellerProfileRepository.save(profile);
        log.info("Updated payment settings for seller: {}", sellerId);

        return SellerPaymentSettingsResponse.builder()
                .id(saved.getId())
                .bankName(saved.getBankName())
                .accountHolderName(saved.getAccountHolderName())
                .accountNumber(maskAccountNumber(saved.getAccountNumber()))
                .branch(saved.getBranch())
                .payoutSchedule(saved.getPayoutSchedule())
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    public SellerShippingSettingsResponse getShippingSettings(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        List<ShippingZoneResponse> zones = shippingZoneRepository.findBySellerIdAndIsActiveTrue(profile.getId())
                .stream()
                .map(this::mapToShippingZoneResponse)
                .collect(Collectors.toList());

        return SellerShippingSettingsResponse.builder()
                .id(profile.getId())
                .returnPolicy(profile.getReturnPolicy())
                .shippingZones(zones)
                .updatedAt(profile.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public SellerShippingSettingsResponse updateShippingSettings(UUID sellerId, SellerShippingSettingsRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        if (request.getReturnPolicy() != null) {
            profile.setReturnPolicy(request.getReturnPolicy());
        }

        SellerProfile saved = sellerProfileRepository.save(profile);
        log.info("Updated shipping settings for seller: {}", sellerId);

        List<ShippingZoneResponse> zones = shippingZoneRepository.findBySellerIdAndIsActiveTrue(saved.getId())
                .stream()
                .map(this::mapToShippingZoneResponse)
                .collect(Collectors.toList());

        return SellerShippingSettingsResponse.builder()
                .id(saved.getId())
                .returnPolicy(saved.getReturnPolicy())
                .shippingZones(zones)
                .updatedAt(saved.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ShippingZoneResponse createShippingZone(UUID sellerId, ShippingZoneRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        ShippingZone zone = ShippingZone.builder()
                .seller(profile)
                .zoneName(request.getZoneName())
                .zoneDescription(request.getZoneDescription())
                .region(request.getRegion())
                .deliveryMethod(request.getDeliveryMethod())
                .shippingCost(request.getShippingCost())
                .freeShippingMin(request.getFreeShippingMin())
                .estimatedDays(request.getEstimatedDays())
                .isActive(true)
                .build();

        ShippingZone saved = shippingZoneRepository.save(zone);
        log.info("Created shipping zone {} for seller: {}", saved.getId(), sellerId);

        return mapToShippingZoneResponse(saved);
    }

    @Override
    @Transactional
    public ShippingZoneResponse updateShippingZone(UUID sellerId, UUID zoneId, ShippingZoneRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        ShippingZone zone = shippingZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found"));

        if (!zone.getSeller().getId().equals(profile.getId())) {
            throw new BadRequestException("Shipping zone does not belong to this seller");
        }

        if (request.getZoneName() != null) zone.setZoneName(request.getZoneName());
        if (request.getZoneDescription() != null) zone.setZoneDescription(request.getZoneDescription());
        if (request.getRegion() != null) zone.setRegion(request.getRegion());
        if (request.getDeliveryMethod() != null) zone.setDeliveryMethod(request.getDeliveryMethod());
        if (request.getShippingCost() != null) zone.setShippingCost(request.getShippingCost());
        if (request.getFreeShippingMin() != null) zone.setFreeShippingMin(request.getFreeShippingMin());
        if (request.getEstimatedDays() != null) zone.setEstimatedDays(request.getEstimatedDays());

        ShippingZone saved = shippingZoneRepository.save(zone);
        log.info("Updated shipping zone {} for seller: {}", zoneId, sellerId);

        return mapToShippingZoneResponse(saved);
    }

    @Override
    @Transactional
    public void deleteShippingZone(UUID sellerId, UUID zoneId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        ShippingZone zone = shippingZoneRepository.findById(zoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Shipping zone not found"));

        if (!zone.getSeller().getId().equals(profile.getId())) {
            throw new BadRequestException("Shipping zone does not belong to this seller");
        }

        zone.setIsActive(false);
        shippingZoneRepository.save(zone);
        log.info("Deleted shipping zone {} for seller: {}", zoneId, sellerId);
    }

    @Override
    public List<ShippingZoneResponse> getShippingZones(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        return shippingZoneRepository.findBySellerId(profile.getId())
                .stream()
                .map(this::mapToShippingZoneResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SellerNotificationSettingsResponse getNotificationSettings(UUID sellerId) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        SellerNotificationSettings settings = sellerNotificationSettingsRepository.findBySellerId(profile.getId())
                .orElseGet(() -> createDefaultNotificationSettings(profile));

        return mapToSellerNotificationSettingsResponse(settings);
    }

    @Override
    @Transactional
    public SellerNotificationSettingsResponse updateNotificationSettings(UUID sellerId, SellerNotificationSettingsRequest request) {
        SellerProfile profile = sellerProfileRepository.findByUserId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller profile not found"));

        SellerNotificationSettings settings = sellerNotificationSettingsRepository.findBySellerId(profile.getId())
                .orElseGet(() -> createDefaultNotificationSettings(profile));

        if (request.getNewOrders() != null) settings.setNewOrders(request.getNewOrders());
        if (request.getOrderUpdates() != null) settings.setOrderUpdates(request.getOrderUpdates());
        if (request.getCustomerMessages() != null) settings.setCustomerMessages(request.getCustomerMessages());
        if (request.getStockAlerts() != null) settings.setStockAlerts(request.getStockAlerts());
        if (request.getPaymentUpdates() != null) settings.setPaymentUpdates(request.getPaymentUpdates());
        if (request.getRefundRequests() != null) settings.setRefundRequests(request.getRefundRequests());
        if (request.getPromotionalEmails() != null) settings.setPromotionalEmails(request.getPromotionalEmails());

        SellerNotificationSettings saved = sellerNotificationSettingsRepository.save(settings);
        log.info("Updated notification settings for seller: {}", sellerId);

        return mapToSellerNotificationSettingsResponse(saved);
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) return accountNumber;
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    private ShippingZoneResponse mapToShippingZoneResponse(ShippingZone zone) {
        return ShippingZoneResponse.builder()
                .id(zone.getId())
                .zoneName(zone.getZoneName())
                .zoneDescription(zone.getZoneDescription())
                .region(zone.getRegion())
                .deliveryMethod(zone.getDeliveryMethod())
                .shippingCost(zone.getShippingCost())
                .freeShippingMin(zone.getFreeShippingMin())
                .estimatedDays(zone.getEstimatedDays())
                .isActive(zone.getIsActive())
                .updatedAt(zone.getUpdatedAt())
                .build();
    }

    private SellerNotificationSettings createDefaultNotificationSettings(SellerProfile profile) {
        return sellerNotificationSettingsRepository.save(
                SellerNotificationSettings.builder()
                        .seller(profile)
                        .newOrders(true)
                        .orderUpdates(true)
                        .customerMessages(true)
                        .stockAlerts(true)
                        .paymentUpdates(true)
                        .refundRequests(true)
                        .promotionalEmails(false)
                        .build()
        );
    }

    private SellerNotificationSettingsResponse mapToSellerNotificationSettingsResponse(SellerNotificationSettings settings) {
        return SellerNotificationSettingsResponse.builder()
                .id(settings.getId())
                .newOrders(settings.getNewOrders())
                .orderUpdates(settings.getOrderUpdates())
                .customerMessages(settings.getCustomerMessages())
                .stockAlerts(settings.getStockAlerts())
                .paymentUpdates(settings.getPaymentUpdates())
                .refundRequests(settings.getRefundRequests())
                .promotionalEmails(settings.getPromotionalEmails())
                .updatedAt(settings.getUpdatedAt())
                .build();
    }
}
