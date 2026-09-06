package ecommerce.graphql.resolver.follow;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.FollowedStoreConnection;
import ecommerce.graphql.dto.FollowerPage;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.modules.follow.dto.FollowStatsResponse;
import ecommerce.modules.follow.dto.FollowedStoreResponse;
import ecommerce.modules.follow.dto.FollowerResponse;
import ecommerce.modules.follow.service.FollowService;
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

import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FollowResolver {

    private final FollowService followService;

    // =========================================================================
    // CUSTOMER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public FollowedStoreConnection myFollowedStores(@Argument PageInput pagination,
                                                    @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myFollowedStores(user={})", principal.getId());
        Pageable pageable = toPageable(pagination);
        Page<FollowedStoreResponse> page = followService.getFollowedStores(principal.getId(), pageable);
        return FollowedStoreConnection.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public boolean isFollowingStore(@Argument UUID sellerId,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL isFollowingStore(user={}, seller={})", principal.getId(), sellerId);
        return followService.isFollowing(principal.getId(), sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public FollowStatsResponse myFollowStats(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myFollowStats(user={})", principal.getId());
        return followService.getCustomerFollowStats(principal.getId());
    }

    // =========================================================================
    // SELLER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public FollowerPage myFollowers(@Argument PageInput pagination,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myFollowers(seller={})", principal.getId());
        Pageable pageable = toPageable(pagination);
        Page<FollowerResponse> page = followService.getFollowers(principal.getId(), pageable);
        return FollowerPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public FollowStatsResponse sellerFollowerStats(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL sellerFollowerStats(seller={})", principal.getId());
        return followService.getSellerFollowStats(principal.getId());
    }

    // =========================================================================
    // PUBLIC QUERIES
    // =========================================================================

    @QueryMapping
    public Long storeFollowerCount(@Argument UUID sellerId) {
        log.info("GQL storeFollowerCount(seller={})", sellerId);
        FollowStatsResponse stats = followService.getSellerFollowStats(sellerId);
        return stats.getTotalFollowers();
    }

    // =========================================================================
    // HELPERS
    // =========================================================================

    private Pageable toPageable(PageInput input) {
        if (input == null) {
            return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "followedAt"));
        }
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }
}
