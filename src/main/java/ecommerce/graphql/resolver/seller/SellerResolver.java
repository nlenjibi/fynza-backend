package ecommerce.graphql.resolver.seller;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
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
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

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
    public SellerDashboardResponse sellerDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerDashboard(seller={})", principal.getId());
        return sellerService.getDashboard(principal.getId());
    }

    // =========================================================================
    // ANALYTICS QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerAnalyticsDto sellerAnalytics(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerAnalytics(seller={})", principal.getId());
        return sellerService.getSellerAnalytics(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerAnalyticsResponse sellerSalesAnalytics(@Argument int days,
                                                        @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerSalesAnalytics(seller={}, days={})", principal.getId(), days);
        return sellerService.getSalesAnalytics(principal.getId(), days);
    }

    // =========================================================================
    // PRODUCT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductDto sellerProducts(@Argument PageInput pagination,
                                     @Argument SellerProductFilterInput filter,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerProducts(seller={})", principal.getId());
        Pageable pageable = toPageable(pagination);

        ProductStatus status = null;
        UUID categoryId = null;
        String search = null;
        if (filter != null) {
            if (filter.getStatus() != null) status = ProductStatus.valueOf(filter.getStatus().toUpperCase());
            categoryId = filter.getCategoryId();
            search = filter.getSearch();
        }

        Page<ProductResponse> page = productService.findBySellerId(principal.getId(), status, categoryId, search, pageable);
        return ProductDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    // =========================================================================
    // STORE QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public StoreResponse sellerStore(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerStore(seller={})", principal.getId());
        return sellerService.getStore(principal.getId());
    }

    // =========================================================================
    // REVIEW QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ReviewResponseDto sellerReviews(@Argument PageInput pagination,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerReviews(seller={})", principal.getId());
        Page<ReviewResponse> page = sellerService.getSellerReviews(principal.getId(), toPageable(pagination));
        return ReviewResponseDto.builder().content(page.getContent()).pageInfo(PaginatedResponse.from(page)).build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public ReviewStatsResponse sellerReviewStats(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerReviewStats(seller={})", principal.getId());
        return reviewService.getSellerReviewStats(principal.getId());
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
    public SellerPaymentSettingsResponse sellerPaymentSettings(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerPaymentSettings(seller={})", principal.getId());
        return sellerService.getPaymentSettings(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerShippingSettingsResponse sellerShippingSettings(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerShippingSettings(seller={})", principal.getId());
        return sellerService.getShippingSettings(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<ShippingZoneResponse> sellerShippingZones(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerShippingZones(seller={})", principal.getId());
        return sellerService.getShippingZones(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerNotificationSettingsResponse sellerNotificationSettings(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerNotificationSettings(seller={})", principal.getId());
        return sellerService.getNotificationSettings(principal.getId());
    }

    // =========================================================================
    // PRODUCT MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse createSellerProduct(@Argument SellerProductCreateInput input,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL createSellerProduct(seller={})", principal.getId());
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
        return productService.create(request, principal.getId());
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ProductResponse updateSellerProduct(@Argument UUID id,
                                               @Argument SellerProductUpdateInput input,
                                               @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateSellerProduct(id={}, seller={})", id, principal.getId());
        ProductResponse existing = productService.findById(id);
        boolean isOwner = existing.getSeller() != null &&
                existing.getSeller().getId().toString().equals(principal.getId().toString());
        if (!isOwner) throw new RuntimeException("You are not authorized to update this product");
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
    public boolean deleteSellerProduct(@Argument UUID id,
                                       @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteSellerProduct(id={}, seller={})", id, principal.getId());
        ProductResponse existing = productService.findById(id);
        boolean isOwner = existing.getSeller() != null &&
                existing.getSeller().getId().toString().equals(principal.getId().toString());
        if (!isOwner) throw new RuntimeException("You are not authorized to delete this product");
        productService.delete(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public boolean assignProductTags(@Argument UUID id,
                                     @Argument List<String> tags,
                                     @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL assignProductTags(id={}, seller={})", id, principal.getId());
        sellerService.assignTagsToProduct(id, tags, principal.getId());
        return true;
    }

    // =========================================================================
    // STORE MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public StoreResponse updateSellerStore(@Argument SellerStoreUpdateInput input,
                                           @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateSellerStore(seller={})", principal.getId());
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
        return sellerService.updateStore(principal.getId(), request);
    }

    // =========================================================================
    // REVIEW MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ReviewResponse sellerReplyToReview(@Argument UUID reviewId,
                                              @Argument SellerReplyInput input,
                                              @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerReplyToReview(review={}, seller={})", reviewId, principal.getId());
        return reviewService.sellerReply(reviewId, principal.getId(), input.getReply());
    }

    // =========================================================================
    // SETTINGS MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerPaymentSettingsResponse updateSellerPaymentSettings(
            @Argument SellerPaymentSettingsInput input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateSellerPaymentSettings(seller={})", principal.getId());
        SellerPaymentSettingsRequest request = SellerPaymentSettingsRequest.builder()
                .bankName(input.getBankName())
                .accountHolderName(input.getAccountHolderName())
                .accountNumber(input.getAccountNumber())
                .branch(input.getBranch())
                .payoutSchedule(ecommerce.common.enums.PayoutSchedule.valueOf(input.getPayoutSchedule()))
                .build();
        return sellerService.updatePaymentSettings(principal.getId(), request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerShippingSettingsResponse updateSellerShippingSettings(
            @Argument SellerShippingSettingsInput input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateSellerShippingSettings(seller={})", principal.getId());
        SellerShippingSettingsRequest request = SellerShippingSettingsRequest.builder()
                .returnPolicy(input.getReturnPolicy())
                .build();
        return sellerService.updateShippingSettings(principal.getId(), request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ShippingZoneResponse createSellerShippingZone(@Argument ShippingZoneInput input,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL createSellerShippingZone(seller={})", principal.getId());
        ShippingZoneRequest request = ShippingZoneRequest.builder()
                .zoneName(input.getZoneName())
                .zoneDescription(input.getZoneDescription())
                .region(input.getRegion())
                .deliveryMethod(ecommerce.modules.seller.entity.ShippingZone.DeliveryMethod.valueOf(input.getDeliveryMethod()))
                .shippingCost(input.getShippingCost())
                .freeShippingMin(input.getFreeShippingMin())
                .estimatedDays(input.getEstimatedDays())
                .build();
        return sellerService.createShippingZone(principal.getId(), request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public ShippingZoneResponse updateSellerShippingZone(@Argument UUID zoneId,
                                                         @Argument ShippingZoneInput input,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateSellerShippingZone(zone={}, seller={})", zoneId, principal.getId());
        ShippingZoneRequest request = ShippingZoneRequest.builder()
                .zoneName(input.getZoneName())
                .zoneDescription(input.getZoneDescription())
                .region(input.getRegion())
                .deliveryMethod(ecommerce.modules.seller.entity.ShippingZone.DeliveryMethod.valueOf(input.getDeliveryMethod()))
                .shippingCost(input.getShippingCost())
                .freeShippingMin(input.getFreeShippingMin())
                .estimatedDays(input.getEstimatedDays())
                .build();
        return sellerService.updateShippingZone(principal.getId(), zoneId, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public boolean deleteSellerShippingZone(@Argument UUID zoneId,
                                            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteSellerShippingZone(zone={}, seller={})", zoneId, principal.getId());
        sellerService.deleteShippingZone(principal.getId(), zoneId);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerNotificationSettingsResponse updateSellerNotificationSettings(
            @Argument SellerNotificationSettingsInput input,
            @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateSellerNotificationSettings(seller={})", principal.getId());
        SellerNotificationSettingsRequest request = SellerNotificationSettingsRequest.builder()
                .newOrders(input.getNewOrders())
                .orderUpdates(input.getOrderUpdates())
                .customerMessages(input.getCustomerMessages())
                .stockAlerts(input.getStockAlerts())
                .paymentUpdates(input.getPaymentUpdates())
                .refundRequests(input.getRefundRequests())
                .promotionalEmails(input.getPromotionalEmails())
                .build();
        return sellerService.updateNotificationSettings(principal.getId(), request);
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
