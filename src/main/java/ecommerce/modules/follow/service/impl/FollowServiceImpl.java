package ecommerce.modules.follow.service.impl;

import ecommerce.common.exception.BadRequestException;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.follow.dto.FollowStatsResponse;
import ecommerce.modules.follow.dto.FollowedStoreResponse;
import ecommerce.modules.follow.dto.FollowerResponse;
import ecommerce.modules.follow.entity.StoreFollow;
import ecommerce.modules.follow.repository.StoreFollowRepository;
import ecommerce.modules.follow.service.FollowService;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.user.entity.SellerProfile;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.SellerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowServiceImpl implements FollowService {

    private final StoreFollowRepository storeFollowRepository;
    private final UserRepository userRepository;
    private final SellerProfileRepository sellerProfileRepository;
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public void followStore(UUID customerId, UUID sellerId) {
        if (storeFollowRepository.existsByCustomerIdAndSellerId(customerId, sellerId)) {
            throw new BadRequestException("Already following this store");
        }

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        SellerProfile seller = sellerProfileRepository.findByPublicId(sellerId)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        StoreFollow follow = StoreFollow.builder()
                .customer(customer)
                .seller(seller)
                .followedAt(LocalDateTime.now())
                .build();

        storeFollowRepository.save(follow);
        log.info("Customer {} followed seller {}", customerId, sellerId);
    }

    @Override
    @Transactional
    public void unfollowStore(UUID customerId, UUID sellerId) {
        StoreFollow follow = storeFollowRepository.findByCustomerIdAndSellerId(customerId, sellerId)
                .orElseThrow(() -> new BadRequestException("Not following this store"));

        storeFollowRepository.delete(follow);
        log.info("Customer {} unfollowed seller {}", customerId, sellerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowerResponse> getFollowers(UUID sellerId, Pageable pageable) {
        return storeFollowRepository.findBySellerId(sellerId, pageable)
                .map(this::mapToFollowerResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FollowedStoreResponse> getFollowedStores(UUID customerId, Pageable pageable) {
        return storeFollowRepository.findByCustomerId(customerId, pageable)
                .map(this::mapToFollowedStoreResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatsResponse getSellerFollowStats(UUID sellerId) {
        long total = storeFollowRepository.countBySellerId(sellerId);
        
        LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
        LocalDateTime startOfWeek = LocalDateTime.now().minusWeeks(1);
        
        long activeThisMonth = storeFollowRepository.countBySellerIdAndFollowedAtSince(sellerId, startOfMonth);
        long newThisWeek = storeFollowRepository.countBySellerIdAndFollowedAtSince(sellerId, startOfWeek);

        return FollowStatsResponse.builder()
                .totalFollowers(total)
                .activeThisMonth(activeThisMonth)
                .newThisWeek(newThisWeek)
                .avgFollowAge(0.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public FollowStatsResponse getCustomerFollowStats(UUID customerId) {
        long total = storeFollowRepository.countByCustomerId(customerId);
        
        return FollowStatsResponse.builder()
                .totalFollowers(total)
                .activeThisMonth(0)
                .newThisWeek(0)
                .avgFollowAge(0.0)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(UUID customerId, UUID sellerId) {
        return storeFollowRepository.existsByCustomerIdAndSellerId(customerId, sellerId);
    }

    private FollowerResponse mapToFollowerResponse(StoreFollow follow) {
        User customer = follow.getCustomer();
        
        long orderCount = orderRepository.findByCustomerId(customer.getId(), Pageable.unpaged()).getTotalElements();
        
        double totalSpent = orderRepository.findByCustomerId(customer.getId(), Pageable.unpaged()).getContent()
                .stream()
                .filter(o -> o.getPaymentStatus() == ecommerce.modules.order.entity.PaymentStatus.PAID)
                .mapToDouble(o -> o.getTotalAmount().doubleValue())
                .sum();

        return FollowerResponse.builder()
                .id(follow.getPublicId())
                .customerId(customer.getId())
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getEmail())
                .customerAvatar(customer.getProfileImageUrl())
                .joinedDate(follow.getFollowedAt())
                .orderCount(orderCount)
                .totalSpent(java.math.BigDecimal.valueOf(totalSpent))
                .lastActive(customer.getLastLoginAt())
                .build();
    }

    private FollowedStoreResponse mapToFollowedStoreResponse(StoreFollow follow) {
        SellerProfile seller = follow.getSeller();
        
        return FollowedStoreResponse.builder()
                .id(follow.getPublicId())
                .sellerId(seller.getPublicId())
                .storeName(seller.getStoreName())
                .storeLogo(seller.getStoreLogo())
                .isVerified(seller.getVerificationStatus() == ecommerce.common.enums.VerificationStatus.VERIFIED)
                .rating(seller.getRating())
                .reviewCount(seller.getTotalReviews())
                .location(seller.getRegion() != null ? seller.getRegion().getDisplayName() : null)
                .productCount(seller.getTotalProducts())
                .followedDate(follow.getFollowedAt())
                .build();
    }
}
