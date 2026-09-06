package ecommerce.graphql.resolver.promotion;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.AdminPromotionPage;
import ecommerce.graphql.dto.AdminPromotionStats;
import ecommerce.graphql.dto.SellerPromotionPage;
import ecommerce.graphql.input.PageInput;
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
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

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
        Page<AdminPromotion> page = adminPromotionService.getPromotions(toPageable(pagination));
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
                                                @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerPromotions(seller={})", principal.getId());
        Page<SellerPromotionDto> page = sellerPromotionService.getPromotions(principal.getId(), toPageable(pagination));
        return SellerPromotionPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<SellerPromotionDto> activeSellerPromotions(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL activeSellerPromotions(seller={})", principal.getId());
        return sellerPromotionService.getActivePromotions(principal.getId());
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
