package ecommerce.modules.follow.service;

import ecommerce.modules.follow.dto.FollowStatsResponse;
import ecommerce.modules.follow.dto.FollowedStoreResponse;
import ecommerce.modules.follow.dto.FollowerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface FollowService {

    void followStore(UUID customerId, UUID sellerId);

    void unfollowStore(UUID customerId, UUID sellerId);

    Page<FollowerResponse> getFollowers(UUID sellerId, Pageable pageable);

    Page<FollowedStoreResponse> getFollowedStores(UUID customerId, Pageable pageable);

    FollowStatsResponse getSellerFollowStats(UUID sellerId);

    FollowStatsResponse getCustomerFollowStats(UUID customerId);

    boolean isFollowing(UUID customerId, UUID sellerId);
}
