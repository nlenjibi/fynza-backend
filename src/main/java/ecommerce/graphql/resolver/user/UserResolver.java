package ecommerce.graphql.resolver.user;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.user.dto.*;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.spec.UserSpec;
import ecommerce.modules.user.service.UserService;
import org.springframework.data.jpa.domain.Specification;
import ecommerce.modules.user.repository.CustomerProfileRepository;
import ecommerce.modules.user.entity.CustomerProfile;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.UserResponceDto;
import ecommerce.graphql.input.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserResolver {

    private final UserService userService;
    private final CustomerProfileRepository customerProfileRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final OrderRepository orderRepository;

    // =========================================================================
    // PUBLIC / AUTHENTICATED QUERIES
    // =========================================================================

    @QueryMapping
    public UserDto user(@Argument UUID id) {
        log.info("GQL user(id={})", id);
        return userService.getUserById(id).orElse(null);
    }

    @QueryMapping
    public UserDto currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL currentUser(user={})", principal.getId());
        return userService.getUserById(principal.getId()).orElse(null);
    }

    // =========================================================================
    // ADMIN/MANAGER USER QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public UserResponceDto users(@Argument PageInput pagination, @Argument UserFilterInput filter) {
        log.info("GQL users");
        Pageable pageable = toPageable(pagination);
        Page<UserDto> userPage = filter != null
                ? userService.findUsersWithPredicate(buildPredicateFromFilter(filter), pageable)
                : userService.getAllUsers(pageable);
        return UserResponceDto.builder()
                .content(userPage.getContent())
                .pageInfo(PaginatedResponse.from(userPage))
                .build();
    }

    // =========================================================================
    // CUSTOMER DASHBOARD QUERY
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerDashboardResponse customerDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL customerDashboard(user={})", principal.getId());
        UUID userId = principal.getId();

        CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId).orElse(null);
        long wishlistItems = wishlistItemRepository.countByUser_PublicId(userId);
        List<Order> orders = orderRepository.findByCustomer_PublicId(userId, PageRequest.of(0, 10)).getContent();
        long totalOrders = orderRepository.findByCustomer_PublicId(userId, PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM d, yyyy");
        List<CustomerDashboardResponse.RecentOrderDto> recentOrders = orders.stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .limit(5)
                .map(order -> CustomerDashboardResponse.RecentOrderDto.builder()
                        .orderId(order.getId().toString())
                        .orderNumber(order.getOrderNumber())
                        .orderDate(order.getCreatedAt() != null ? order.getCreatedAt().format(formatter) : "N/A")
                        .status(order.getStatus().name())
                        .totalAmount(order.getTotalAmount())
                        .build())
                .collect(Collectors.toList());

        return CustomerDashboardResponse.builder()
                .totalOrders(totalOrders)
                .wishlistItems(wishlistItems)
                .savedAddresses((long) userService.getCustomerAddresses(userId).size())
                .loyaltyPoints(customerProfile != null ? customerProfile.getLoyaltyPoints() : 0)
                .totalSpent(customerProfile != null ? customerProfile.getTotalSpent() : BigDecimal.ZERO)
                .membershipStatus(customerProfile != null ? customerProfile.getMembershipStatus().name() : "NONE")
                .recentOrders(recentOrders)
                .build();
    }

    // =========================================================================
    // ADDRESS QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<AddressDto> myAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL myAddresses(user={})", principal.getId());
        return userService.getCustomerAddresses(principal.getId());
    }

    // =========================================================================
    // LOYALTY QUERY
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoyaltyRedemptionResponse loyaltyBalance(@AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL loyaltyBalance(user={})", principal.getId());
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(principal.getId())
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        int points = customerProfile.getLoyaltyPoints() != null ? customerProfile.getLoyaltyPoints() : 0;
        BigDecimal availableDiscount = BigDecimal.valueOf(points).multiply(BigDecimal.valueOf(0.10));
        return LoyaltyRedemptionResponse.builder()
                .previousPoints(0)
                .redeemedPoints(0)
                .remainingPoints(points)
                .discountAmount(availableDiscount)
                .rewardType("AVAILABLE")
                .message(points + " points available - " + availableDiscount + " worth of discounts")
                .build();
    }

    // =========================================================================
    // CUSTOMER PROFILE MUTATION
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public UserDto updateCustomerProfile(@Argument CustomerProfileUpdateInput input,
                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateCustomerProfile(user={})", principal.getId());
        UserDto request = UserDto.builder()
                .email(input.getEmail())
                .username(input.getUsername())
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .phone(input.getPhone())
                .build();
        return userService.updateCustomerProfile(principal.getId(), request);
    }

    // =========================================================================
    // ADDRESS MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public AddressDto addAddress(@Argument AddressInput input,
                                 @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL addAddress(user={})", principal.getId());
        AddressRequest request = AddressRequest.builder()
                .label(input.getLabel())
                .type(input.getAddressType())
                .streetAddress(input.getStreetAddress())
                .city(input.getCity())
                .state(input.getState())
                .postalCode(input.getPostalCode())
                .country(input.getCountry())
                .isDefault(input.getIsDefault())
                .build();
        return userService.addCustomerAddress(principal.getId(), request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public AddressDto updateAddress(@Argument UUID id,
                                    @Argument AddressInput input,
                                    @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL updateAddress(id={}, user={})", id, principal.getId());
        AddressRequest request = AddressRequest.builder()
                .label(input.getLabel())
                .type(input.getAddressType())
                .streetAddress(input.getStreetAddress())
                .city(input.getCity())
                .state(input.getState())
                .postalCode(input.getPostalCode())
                .country(input.getCountry())
                .isDefault(input.getIsDefault())
                .build();
        return userService.updateCustomerAddress(principal.getId(), id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public boolean deleteAddress(@Argument UUID id, @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL deleteAddress(id={}, user={})", id, principal.getId());
        userService.deleteCustomerAddress(principal.getId(), id);
        return true;
    }

    // =========================================================================
    // LOYALTY MUTATION
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoyaltyRedemptionResponse redeemLoyaltyPoints(@Argument LoyaltyRedeemInput input,
                                                         @AuthenticationPrincipal UserPrincipal principal) {
        log.info("GQL redeemLoyaltyPoints(user={}, points={})", principal.getId(), input.getPointsToRedeem());
        UUID userId = principal.getId();
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));

        Integer currentPoints = customerProfile.getLoyaltyPoints();
        int pointsToRedeem = input.getPointsToRedeem();
        if (currentPoints == null || currentPoints < 100 || pointsToRedeem < 100 || pointsToRedeem > currentPoints) {
            throw new RuntimeException("Insufficient loyalty points for redemption");
        }

        BigDecimal discountAmount = BigDecimal.valueOf(pointsToRedeem).multiply(BigDecimal.valueOf(0.10));
        customerProfile.setLoyaltyPoints(currentPoints - pointsToRedeem);
        customerProfileRepository.save(customerProfile);

        String rewardType = input.getRewardType() != null ? input.getRewardType() : "DISCOUNT";
        String couponCode = "LOYALTY-" + pointsToRedeem + "-" + userId.toString().substring(0, 8).toUpperCase();

        return LoyaltyRedemptionResponse.builder()
                .previousPoints(currentPoints)
                .redeemedPoints(pointsToRedeem)
                .remainingPoints(currentPoints - pointsToRedeem)
                .discountAmount(discountAmount)
                .rewardType(rewardType)
                .couponCode(couponCode)
                .message("Successfully redeemed " + pointsToRedeem + " points for " + discountAmount + " discount!")
                .build();
    }

    // =========================================================================
    // FIELD RESOLVERS
    // =========================================================================

    @SchemaMapping(typeName = "User")
    public String fullName(UserDto user) {
        return user.getFullName();
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

    private Specification<User> buildPredicateFromFilter(UserFilterInput filter) {
        return Specification.where(UserSpec.emailOrNameContains(filter.getSearch()))
                .and(filter.getRole() != null ? UserSpec.hasRole(filter.getRole()) : null)
                .and(Boolean.TRUE.equals(filter.getActive()) ? UserSpec.isActive() : null);
    }
}
