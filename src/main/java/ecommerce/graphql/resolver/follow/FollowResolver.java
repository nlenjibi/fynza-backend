package ecommerce.graphql.resolver.follow;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.FollowedStoreConnection;
import ecommerce.graphql.dto.FollowerPage;
import ecommerce.graphql.input.FollowInput;
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
import org.springframework.graphql.data.method.annotation.ContextValue;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
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
                                                     @ContextValue UUID userId) {
        log.info("GQL myFollowedStores(user={})", userId);
        Pageable pageable = toPageable(pagination);
        Page<FollowedStoreResponse> page = followService.getFollowedStores(userId, pageable);
        return FollowedStoreConnection.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public boolean isFollowingStore(@Argument UUID sellerId,
                                     @ContextValue UUID userId) {
        log.info("GQL isFollowingStore(user={}, seller={})", userId, sellerId);
        return followService.isFollowing(userId, sellerId);
    }

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public FollowStatsResponse myFollowStats(@ContextValue UUID userId) {
        log.info("GQL myFollowStats(user={})", userId);
        return followService.getCustomerFollowStats(userId);
    }

    // =========================================================================
    // SELLER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public FollowerPage myFollowers(@Argument PageInput pagination,
                                     @ContextValue UUID sellerId) {
        log.info("GQL myFollowers(seller={})", sellerId);
        Pageable pageable = toPageable(pagination);
        Page<FollowerResponse> page = followService.getFollowers(sellerId, pageable);
        return FollowerPage.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('SELLER')")
    public FollowStatsResponse sellerFollowerStats(@ContextValue UUID sellerId) {
        log.info("GQL sellerFollowerStats(seller={})", sellerId);
        return followService.getSellerFollowStats(sellerId);
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
    // CUSTOMER MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public boolean followStore(@Argument FollowInput input,
                                @ContextValue UUID userId) {
        log.info("GQL followStore(user={}, seller={})", userId, input.getSellerId());
        followService.followStore(userId, input.getSellerId());
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public boolean unfollowStore(@Argument UUID sellerId,
                                  @ContextValue UUID userId) {
        log.info("GQL unfollowStore(user={}, seller={})", userId, sellerId);
        followService.unfollowStore(userId, sellerId);
        return true;
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
