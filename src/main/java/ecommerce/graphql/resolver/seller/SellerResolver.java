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
