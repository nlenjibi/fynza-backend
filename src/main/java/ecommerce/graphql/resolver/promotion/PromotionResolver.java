package ecommerce.graphql.resolver.promotion;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.AdminPromotionPage;
import ecommerce.graphql.dto.AdminPromotionStats;
import ecommerce.graphql.dto.SellerPromotionPage;
import ecommerce.graphql.input.AdminPromotionCreateInput;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SellerPromotionCreateInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.promotion.entity.AdminPromotion;
import ecommerce.modules.promotion.dto.SellerPromotionDto;
import ecommerce.modules.promotion.service.AdminPromotionService;
import ecommerce.modules.promotion.service.SellerPromotionService;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@Slf4j
public class PromotionResolver {

    private final AdminPromotionService adminPromotionService;
    private final SellerPromotionService sellerPromotionService;

    // =========================================================================
    // ADMIN QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPromotionPage adminPromotions(@Argument PageInput pagination) {
        log.info("GQL adminPromotions");
        Pageable pageable = toPageable(pagination);
        Page<AdminPromotion> page = adminPromotionService.getPromotions(pageable);
        return AdminPromotionPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminPromotion> activeAdminPromotions() {
        log.info("GQL activeAdminPromotions");
        return adminPromotionService.getActivePromotions();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPromotionStats adminPromotionStats() {
        log.info("GQL adminPromotionStats");
        return AdminPromotionStats.builder()
                .activeCount(adminPromotionService.countActivePromotions())
                .totalRevenue(adminPromotionService.getTotalRevenue())
                .build();
    }

    // =========================================================================
    // SELLER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerPromotionPage sellerPromotions(@Argument PageInput pagination,
                                                  @ContextValue UUID sellerId) {
        log.info("GQL sellerPromotions(seller={})", sellerId);
        Pageable pageable = toPageable(pagination);
        Page<SellerPromotionDto> page = sellerPromotionService.getPromotions(sellerId, pageable);
        return SellerPromotionPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<SellerPromotionDto> activeSellerPromotions(@ContextValue UUID sellerId) {
        log.info("GQL activeSellerPromotions(seller={})", sellerId);
        return sellerPromotionService.getActivePromotions(sellerId);
    }

    // =========================================================================
    // ADMIN MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminPromotion createAdminPromotion(@Argument AdminPromotionCreateInput input,
                                                @ContextValue UUID userId) {
        log.info("GQL createAdminPromotion(admin={})", userId);
        return adminPromotionService.createPromotion(
                userId,
                input.getName(),
                AdminPromotion.PromotionType.valueOf(input.getPromotionType().toUpperCase()),
                input.getCode(),
                input.getDiscountValue(),
                input.getMinPurchase(),
                input.getMaxDiscount(),
                input.getStartDate(),
                input.getEndDate(),
                input.getUsageLimit(),
                input.getCategoryId(),
                input.getIsGlobal() != null ? input.getIsGlobal() : false,
                "0.0.0.0"
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteAdminPromotion(@Argument UUID id, @ContextValue UUID userId) {
        log.info("GQL deleteAdminPromotion(id={}, admin={})", id, userId);
        adminPromotionService.deletePromotion(userId, id, "0.0.0.0");
        return true;
    }

    // =========================================================================
    // SELLER MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public SellerPromotionDto createSellerPromotion(@Argument SellerPromotionCreateInput input,
                                                      @ContextValue UUID sellerId) {
        log.info("GQL createSellerPromotion(seller={})", sellerId);
        return sellerPromotionService.createPromotion(
                sellerId,
                input.getName(),
                input.getPromotionType(),
                input.getDiscountValue(),
                input.getMinPurchase(),
                input.getStartDate(),
                input.getEndDate(),
                input.getUsageLimit(),
                "0.0.0.0"
        );
    }

    @MutationMapping
    @PreAuthorize("hasRole('SELLER')")
    public boolean deleteSellerPromotion(@Argument UUID id, @ContextValue UUID sellerId) {
        log.info("GQL deleteSellerPromotion(id={}, seller={})", id, sellerId);
        sellerPromotionService.deletePromotion(sellerId, id, "0.0.0.0");
        return true;
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
