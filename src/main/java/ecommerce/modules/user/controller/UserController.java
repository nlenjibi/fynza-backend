package ecommerce.modules.user.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.user.dto.AddressDto;
import ecommerce.modules.user.dto.AddressRequest;
import ecommerce.modules.user.dto.CustomerDashboardResponse;
import ecommerce.modules.user.dto.LoyaltyRedemptionRequest;
import ecommerce.modules.user.dto.LoyaltyRedemptionResponse;
import ecommerce.modules.user.dto.UserDto;
import ecommerce.modules.user.entity.CustomerProfile;
import ecommerce.modules.user.repository.AddressRepository;
import ecommerce.modules.user.repository.CustomerProfileRepository;
import ecommerce.modules.user.repository.UserRepository;
import ecommerce.modules.user.service.UserService;
import ecommerce.modules.wishlist.repository.WishlistItemRepository;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customer Management", description = "Customer profile and address management endpoints")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final OrderRepository orderRepository;
    private final AddressRepository addressRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer dashboard", description = "Retrieve the authenticated customer's dashboard data")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getCustomerDashboard(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId).orElse(null);
        
        long wishlistItems = wishlistItemRepository.countByUserId(userId);
        
        List<Order> orders = orderRepository.findByCustomerId(userId, PageRequest.of(0, 10)).getContent();
        long totalOrders = orderRepository.findByCustomerId(userId, PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements();
        
        long savedAddresses = addressRepository.findByUserId(userId).size();
        
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
        
        CustomerDashboardResponse response = CustomerDashboardResponse.builder()
                .totalOrders(totalOrders)
                .wishlistItems(wishlistItems)
                .savedAddresses((long) savedAddresses)
                .loyaltyPoints(customerProfile != null ? customerProfile.getLoyaltyPoints() : 0)
                .totalSpent(customerProfile != null ? customerProfile.getTotalSpent() : java.math.BigDecimal.ZERO)
                .membershipStatus(customerProfile != null ? customerProfile.getMembershipStatus().name() : "NONE")
                .recentOrders(recentOrders)
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved successfully", response));
    }

    @PostMapping("/loyalty/redeem")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Redeem loyalty points", description = "Redeem loyalty points for discounts or rewards")
    public ResponseEntity<ApiResponse<LoyaltyRedemptionResponse>> redeemLoyaltyPoints(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody LoyaltyRedemptionRequest request) {
        
        UUID userId = principal.getId();
        
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        
        Integer currentPoints = customerProfile.getLoyaltyPoints();
        
        if (currentPoints == null || currentPoints < 100) {
            return ResponseEntity.badRequest().body(ApiResponse.<LoyaltyRedemptionResponse>builder()
                    .message("Insufficient loyalty points. Minimum 100 points required for redemption.")
                    .statusCode(400)
                    .build());
        }
        
        int pointsToRedeem = request.getPointsToRedeem();
        
        if (pointsToRedeem < 100) {
            return ResponseEntity.badRequest().body(ApiResponse.<LoyaltyRedemptionResponse>builder()
                    .message("Minimum 100 points required for redemption")
                    .statusCode(400)
                    .build());
        }
        
        if (pointsToRedeem > currentPoints) {
            return ResponseEntity.badRequest().body(ApiResponse.<LoyaltyRedemptionResponse>builder()
                    .message("Insufficient loyalty points. You have " + currentPoints + " points.")
                    .statusCode(400)
                    .build());
        }
        
        BigDecimal discountAmount = calculateDiscount(pointsToRedeem);
        
        customerProfile.setLoyaltyPoints(currentPoints - pointsToRedeem);
        customerProfileRepository.save(customerProfile);
        
        String rewardType = request.getRewardType() != null ? request.getRewardType() : "DISCOUNT";
        String couponCode = generateCouponCode(userId, pointsToRedeem);
        
        LoyaltyRedemptionResponse response = LoyaltyRedemptionResponse.builder()
                .previousPoints(currentPoints)
                .redeemedPoints(pointsToRedeem)
                .remainingPoints(currentPoints - pointsToRedeem)
                .discountAmount(discountAmount)
                .rewardType(rewardType)
                .couponCode(couponCode)
                .message("Successfully redeemed " + pointsToRedeem + " points for " + discountAmount + " discount!")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Loyalty points redeemed successfully", response));
    }

    @GetMapping("/loyalty/balance")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get loyalty points balance", description = "Get current loyalty points balance and membership status")
    public ResponseEntity<ApiResponse<LoyaltyRedemptionResponse>> getLoyaltyBalance(
            @AuthenticationPrincipal UserPrincipal principal) {
        
        UUID userId = principal.getId();
        
        CustomerProfile customerProfile = customerProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Customer profile not found"));
        
        int points = customerProfile.getLoyaltyPoints() != null ? customerProfile.getLoyaltyPoints() : 0;
        BigDecimal availableDiscount = calculateDiscount(points);
        
        LoyaltyRedemptionResponse response = LoyaltyRedemptionResponse.builder()
                .previousPoints(0)
                .redeemedPoints(0)
                .remainingPoints(points)
                .discountAmount(availableDiscount)
                .rewardType("AVAILABLE")
                .message(points + " points available - " + availableDiscount + " worth of discounts")
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Loyalty balance retrieved successfully", response));
    }

    private BigDecimal calculateDiscount(int points) {
        return BigDecimal.valueOf(points).multiply(BigDecimal.valueOf(0.10));
    }

    private String generateCouponCode(UUID userId, int points) {
        return "LOYALTY-" + points + "-" + userId.toString().substring(0, 8).toUpperCase();
    }

    @GetMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer profile", description = "Retrieve the authenticated customer's profile")
    public ResponseEntity<ApiResponse<UserDto>> getCustomerProfile(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        UserDto userDto = userService.getCustomerProfile(userId);
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", userDto));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update customer profile", description = "Update the authenticated customer's profile")
    public ResponseEntity<ApiResponse<UserDto>> updateCustomerProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserDto request) {
        UUID userId = principal.getId();
        UserDto userDto = userService.updateCustomerProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", userDto));
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get customer addresses", description = "Retrieve all addresses for the authenticated customer")
    public ResponseEntity<ApiResponse<java.util.List<AddressDto>>> getCustomerAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        java.util.List<AddressDto> addresses = userService.getCustomerAddresses(userId);
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
    }

    @PostMapping("/addresses")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Add customer address", description = "Add a new address for the authenticated customer")
    public ResponseEntity<ApiResponse<AddressDto>> addCustomerAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        UUID userId = principal.getId();
        AddressDto address = userService.addCustomerAddress(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Address added successfully", address));
    }

    @PutMapping("/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Update customer address", description = "Update an existing address for the authenticated customer")
    public ResponseEntity<ApiResponse<AddressDto>> updateCustomerAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody AddressRequest request) {
        UUID userId = principal.getId();
        AddressDto address = userService.updateCustomerAddress(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", address));
    }

    @DeleteMapping("/addresses/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Delete customer address", description = "Delete an address for the authenticated customer")
    public ResponseEntity<ApiResponse<Void>> deleteCustomerAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        UUID userId = principal.getId();
        userService.deleteCustomerAddress(userId, id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }
}
