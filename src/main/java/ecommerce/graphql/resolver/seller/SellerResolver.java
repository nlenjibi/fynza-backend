package ecommerce.graphql.resolver.seller;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.ProductDto;
import ecommerce.graphql.dto.ReviewResponseDto;
import ecommerce.graphql.input.*;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.OrderStatusUpdateRequest;
import ecommerce.modules.order.dto.SellerOrderStatsResponse;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.dto.CreateProductRequest;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.dto.SellerProductStatsResponse;
import ecommerce.modules.product.dto.UpdateProductRequest;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.review.dto.ReviewResponse;
import ecommerce.modules.review.dto.ReviewStatsResponse;
import ecommerce.modules.review.service.ReviewService;
import ecommerce.modules.seller.dto.*;
import ecommerce.modules.seller.service.SellerService;
import ecommerce.modules.tag.dto.TagResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class SellerResolver {

    private final SellerService sellerService;
    private final ProductService productService;
    private final OrderService orderService;
    private final ReviewService reviewService;

    // =========================================================================
    // DASHBOARD QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerDashboardResponse sellerDashboard(@ContextValue UUID sellerId) {
        log.info("GQL sellerDashboard(seller={})", sellerId);
        return sellerService.getDashboard(sellerId);
    }

    // =========================================================================
    // ANALYTICS QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerAnalyticsDto sellerAnalytics(@ContextValue UUID sellerId) {
        log.info("GQL sellerAnalytics(seller={})", sellerId);
        return sellerService.getSellerAnalytics(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerAnalyticsResponse sellerSalesAnalytics(
            @Argument int days,
            @ContextValue UUID sellerId) {
        log.info("GQL sellerSalesAnalytics(seller={}, days={})", sellerId, days);
        return sellerService.getSalesAnalytics(sellerId, days);
    }

    // =========================================================================
    // PRODUCT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductDto sellerProducts(
            @Argument PageInput pagination,
            @Argument SellerProductFilterInput filter,
            @ContextValue UUID sellerId) {
        log.info("GQL sellerProducts(seller={})", sellerId);
        Pageable pageable = toPageable(pagination);

        ProductStatus status = null;
        UUID categoryId = null;
        String search = null;

        if (filter != null) {
            if (filter.getStatus() != null) {
                status = ProductStatus.valueOf(filter.getStatus().toUpperCase());
            }
            categoryId = filter.getCategoryId();
            search = filter.getSearch();
        }

        Page<ProductResponse> page = productService.findBySellerId(sellerId, status, categoryId, search, pageable);
        return ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    // =========================================================================
    // STORE QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public StoreResponse sellerStore(@ContextValue UUID sellerId) {
        log.info("GQL sellerStore(seller={})", sellerId);
        return sellerService.getStore(sellerId);
    }

    // =========================================================================
    // REVIEW QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ReviewResponseDto sellerReviews(
            @Argument PageInput pagination,
            @ContextValue UUID sellerId) {
        log.info("GQL sellerReviews(seller={})", sellerId);
        Pageable pageable = toPageable(pagination);
        Page<ReviewResponse> page = sellerService.getSellerReviews(sellerId, pageable);
        return ReviewResponseDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ReviewStatsResponse sellerReviewStats(@ContextValue UUID sellerId) {
        log.info("GQL sellerReviewStats(seller={})", sellerId);
        return reviewService.getSellerReviewStats(sellerId);
    }

    // =========================================================================
    // TAG QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<TagResponse> sellerTags() {
        log.info("GQL sellerTags");
        return sellerService.getTags();
    }

    // =========================================================================
    // SETTINGS QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerPaymentSettingsResponse sellerPaymentSettings(@ContextValue UUID sellerId) {
        log.info("GQL sellerPaymentSettings(seller={})", sellerId);
        return sellerService.getPaymentSettings(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerShippingSettingsResponse sellerShippingSettings(@ContextValue UUID sellerId) {
        log.info("GQL sellerShippingSettings(seller={})", sellerId);
        return sellerService.getShippingSettings(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<ShippingZoneResponse> sellerShippingZones(@ContextValue UUID sellerId) {
        log.info("GQL sellerShippingZones(seller={})", sellerId);
        return sellerService.getShippingZones(sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerNotificationSettingsResponse sellerNotificationSettings(@ContextValue UUID sellerId) {
        log.info("GQL sellerNotificationSettings(seller={})", sellerId);
        return sellerService.getNotificationSettings(sellerId);
    }

    // =========================================================================
    // PRODUCT MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse createSellerProduct(
            @Argument SellerProductCreateInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL createSellerProduct(seller={})", sellerId);
        CreateProductRequest request = CreateProductRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .brand(input.getBrand())
                .sku(input.getSku())
                .price(input.getPrice())
                .originalPrice(input.getOriginalPrice())
                .categoryId(input.getCategoryId())
                .stock(input.getStock())
                .images(input.getImages())
                .build();
        return productService.create(request, sellerId);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse updateSellerProduct(
            @Argument UUID id,
            @Argument SellerProductUpdateInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL updateSellerProduct(id={}, seller={})", id, sellerId);

        ProductResponse existing = productService.findById(id);
        boolean isOwner = existing.getSeller() != null &&
                existing.getSeller().getId().toString().equals(sellerId.toString());
        if (!isOwner) {
            throw new RuntimeException("You are not authorized to update this product");
        }

        UpdateProductRequest request = UpdateProductRequest.builder()
                .name(input.getName())
                .description(input.getDescription())
                .brand(input.getBrand())
                .sku(input.getSku())
                .price(input.getPrice())
                .originalPrice(input.getOriginalPrice())
                .categoryId(input.getCategoryId())
                .stock(input.getStock())
                .images(input.getImages())
                .build();
        return productService.update(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public boolean deleteSellerProduct(@Argument UUID id, @ContextValue UUID sellerId) {
        log.info("GQL deleteSellerProduct(id={}, seller={})", id, sellerId);

        ProductResponse existing = productService.findById(id);
        boolean isOwner = existing.getSeller() != null &&
                existing.getSeller().getId().toString().equals(sellerId.toString());
        if (!isOwner) {
            throw new RuntimeException("You are not authorized to delete this product");
        }

        productService.delete(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public boolean assignProductTags(
            @Argument UUID id,
            @Argument List<String> tags,
            @ContextValue UUID sellerId) {
        log.info("GQL assignProductTags(id={}, seller={})", id, sellerId);
        sellerService.assignTagsToProduct(id, tags, sellerId);
        return true;
    }

    // =========================================================================
    // STORE MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public StoreResponse updateSellerStore(
            @Argument SellerStoreUpdateInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL updateSellerStore(seller={})", sellerId);
        UpdateStoreRequest request = UpdateStoreRequest.builder()
                .storeName(input.getStoreName())
                .storeDescription(input.getStoreDescription())
                .storeWebsite(input.getStoreWebsite())
                .storeLogo(input.getStoreLogo())
                .storeBanner(input.getStoreBanner())
                .email(input.getEmail())
                .phone(input.getPhone())
                .city(input.getCity())
                .businessAddress(input.getBusinessAddress())
                .workingHours(input.getWorkingHours())
                .facebookUrl(input.getFacebookUrl())
                .instagramUrl(input.getInstagramUrl())
                .twitterUrl(input.getTwitterUrl())
                .businessRegistration(input.getBusinessRegistration())
                .bankName(input.getBankName())
                .build();
        return sellerService.updateStore(sellerId, request);
    }

    // =========================================================================
    // REVIEW MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ReviewResponse sellerReplyToReview(
            @Argument UUID reviewId,
            @Argument SellerReplyInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL sellerReplyToReview(review={}, seller={})", reviewId, sellerId);
        return reviewService.sellerReply(reviewId, sellerId, input.getReply());
    }

    // =========================================================================
    // SETTINGS MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerPaymentSettingsResponse updateSellerPaymentSettings(
            @Argument SellerPaymentSettingsInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL updateSellerPaymentSettings(seller={})", sellerId);
        SellerPaymentSettingsRequest request = SellerPaymentSettingsRequest.builder()
                .bankName(input.getBankName())
                .accountHolderName(input.getAccountHolderName())
                .accountNumber(input.getAccountNumber())
                .branch(input.getBranch())
                .payoutSchedule(ecommerce.common.enums.PayoutSchedule.valueOf(input.getPayoutSchedule()))
                .build();
        return sellerService.updatePaymentSettings(sellerId, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerShippingSettingsResponse updateSellerShippingSettings(
            @Argument SellerShippingSettingsInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL updateSellerShippingSettings(seller={})", sellerId);
        SellerShippingSettingsRequest request = SellerShippingSettingsRequest.builder()
                .returnPolicy(input.getReturnPolicy())
                .build();
        return sellerService.updateShippingSettings(sellerId, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ShippingZoneResponse createSellerShippingZone(
            @Argument ShippingZoneInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL createSellerShippingZone(seller={})", sellerId);
        ShippingZoneRequest request = ShippingZoneRequest.builder()
                .zoneName(input.getZoneName())
                .zoneDescription(input.getZoneDescription())
                .region(input.getRegion())
                .deliveryMethod(ecommerce.modules.seller.entity.ShippingZone.DeliveryMethod.valueOf(input.getDeliveryMethod()))
                .shippingCost(input.getShippingCost())
                .freeShippingMin(input.getFreeShippingMin())
                .estimatedDays(input.getEstimatedDays())
                .build();
        return sellerService.createShippingZone(sellerId, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ShippingZoneResponse updateSellerShippingZone(
            @Argument UUID zoneId,
            @Argument ShippingZoneInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL updateSellerShippingZone(zone={}, seller={})", zoneId, sellerId);
        ShippingZoneRequest request = ShippingZoneRequest.builder()
                .zoneName(input.getZoneName())
                .zoneDescription(input.getZoneDescription())
                .region(input.getRegion())
                .deliveryMethod(ecommerce.modules.seller.entity.ShippingZone.DeliveryMethod.valueOf(input.getDeliveryMethod()))
                .shippingCost(input.getShippingCost())
                .freeShippingMin(input.getFreeShippingMin())
                .estimatedDays(input.getEstimatedDays())
                .build();
        return sellerService.updateShippingZone(sellerId, zoneId, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public boolean deleteSellerShippingZone(
            @Argument UUID zoneId,
            @ContextValue UUID sellerId) {
        log.info("GQL deleteSellerShippingZone(zone={}, seller={})", zoneId, sellerId);
        sellerService.deleteShippingZone(sellerId, zoneId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerNotificationSettingsResponse updateSellerNotificationSettings(
            @Argument SellerNotificationSettingsInput input,
            @ContextValue UUID sellerId) {
        log.info("GQL updateSellerNotificationSettings(seller={})", sellerId);
        SellerNotificationSettingsRequest request = SellerNotificationSettingsRequest.builder()
                .newOrders(input.getNewOrders())
                .orderUpdates(input.getOrderUpdates())
                .customerMessages(input.getCustomerMessages())
                .stockAlerts(input.getStockAlerts())
                .paymentUpdates(input.getPaymentUpdates())
                .refundRequests(input.getRefundRequests())
                .promotionalEmails(input.getPromotionalEmails())
                .build();
        return sellerService.updateNotificationSettings(sellerId, request);
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
