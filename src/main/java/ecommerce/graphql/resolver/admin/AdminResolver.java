package ecommerce.graphql.resolver.admin;

import com.querydsl.core.types.Predicate;
import ecommerce.common.enums.ProductStatus;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.CustomerStats;
import ecommerce.graphql.dto.SellerStats;
import ecommerce.graphql.dto.UserResponceDto;
import ecommerce.graphql.input.*;
import ecommerce.modules.admin.dto.AdminAnalyticsDto;
import ecommerce.modules.admin.dto.AdminDashboardDto;
import ecommerce.modules.admin.service.AdminService;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.service.OrderService;
import ecommerce.modules.product.dto.AdminProductStatsResponse;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.user.dto.*;
import ecommerce.modules.user.entity.UserPredicates;
import ecommerce.modules.user.service.UserService;
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
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminResolver {

    private final AdminService adminService;
    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    // =========================================================================
    // DASHBOARD QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public AdminDashboardDto adminDashboard() {
        log.info("GQL adminDashboard");
        return adminService.getDashboardStats();
    }

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public AdminDashboardDto staffDashboard() {
        log.info("GQL staffDashboard");
        return adminService.getDashboardStats();
    }

    // =========================================================================
    // USER MANAGEMENT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public UserResponceDto allUsers(@Argument PageInput pagination,
                                     @Argument UserFilterInput filter) {
        log.info("GQL allUsers");
        Pageable pageable = toPageable(pagination);

        Page<UserDto> userPage;
        if (filter != null) {
            Predicate predicate = buildPredicateFromFilter(filter);
            userPage = userService.findUsersWithPredicate(predicate, pageable);
        } else {
            userPage = userService.getAllUsers(pageable);
        }

        return UserResponceDto.builder()
                .content(userPage.getContent())
                .pageInfo(PaginatedResponse.from(userPage))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto adminUser(@Argument UUID id) {
        log.info("GQL adminUser(id={})", id);
        return userService.getUserById(id).orElse(null);
    }

    // =========================================================================
    // CUSTOMER MANAGEMENT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public CustomerStats customerStats() {
        log.info("GQL customerStats");
        Map<String, Object> stats = userService.getCustomerStats();
        return CustomerStats.builder()
                .totalCustomers((Long) stats.getOrDefault("totalCustomers", 0L))
                .activeCustomers((Long) stats.getOrDefault("activeCustomers", 0L))
                .newCustomersThisMonth((Long) stats.getOrDefault("newCustomersThisMonth", 0L))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponceDto searchCustomers(@Argument CustomerSearchInput filter,
                                            @Argument PageInput pagination) {
        log.info("GQL searchCustomers");
        Pageable pageable = toPageable(pagination);
        String query = filter != null ? filter.getQuery() : null;
        String status = filter != null ? filter.getStatus() : null;
        Page<UserDto> page = userService.searchCustomers(query, status, pageable);
        return UserResponceDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    // =========================================================================
    // SELLER MANAGEMENT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponceDto adminSellers(@Argument SellerSearchInput filter,
                                         @Argument PageInput pagination) {
        log.info("GQL adminSellers");
        Pageable pageable = toPageable(pagination);
        String query = filter != null ? filter.getQuery() : null;
        String status = filter != null ? filter.getStatus() : null;
        Page<UserDto> page = userService.searchSellers(query, status, pageable);
        return UserResponceDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public SellerStats sellerStats() {
        log.info("GQL sellerStats");
        Map<String, Object> stats = userService.getSellerStats();
        return SellerStats.builder()
                .totalSellers((Long) stats.getOrDefault("totalSellers", 0L))
                .activeSellers((Long) stats.getOrDefault("activeSellers", 0L))
                .pendingSellers((Long) stats.getOrDefault("pendingSellers", 0L))
                .suspendedSellers((Long) stats.getOrDefault("suspendedSellers", 0L))
                .build();
    }

    // =========================================================================
    // PRODUCT MANAGEMENT QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ecommerce.graphql.dto.ProductDto adminProducts(@Argument PageInput pagination,
                                                           @Argument AdminProductSearchInput filter) {
        log.info("GQL adminProducts");
        Pageable pageable = toPageable(pagination);
        ProductStatus status = null;
        String search = null;
        if (filter != null) {
            if (filter.getStatus() != null) {
                status = ProductStatus.valueOf(filter.getStatus().toUpperCase());
            }
            search = filter.getSearch();
        }
        Page<ProductResponse> page = productService.findBySellerId(null, status, null, search, pageable);
        return ecommerce.graphql.dto.ProductDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    // =========================================================================
    // USER CRUD MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto createUser(@Argument AdminUserCreateInput input) {
        log.info("GQL createUser(email={})", input.getEmail());
        UserCreateRequest request = UserCreateRequest.builder()
                .username(input.getUsername())
                .email(input.getEmail())
                .password(input.getPassword())
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .phone(input.getPhone())
                .role(input.getRole())
                .build();
        return userService.createUser(request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUser(@Argument UUID id,
                               @Argument AdminUserUpdateInput input) {
        log.info("GQL updateUser(id={})", id);
        UserUpdateRequest request = UserUpdateRequest.builder()
                .username(input.getUsername())
                .email(input.getEmail())
                .firstName(input.getFirstName())
                .lastName(input.getLastName())
                .phone(input.getPhone())
                .role(input.getRole())
                .isActive(input.getIsActive())
                .build();
        return userService.updateUser(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteUser(@Argument UUID id) {
        log.info("GQL deleteUser(id={})", id);
        userService.deleteUser(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserRole(@Argument UUID id,
                                   @Argument String role) {
        log.info("GQL updateUserRole(id={}, role={})", id, role);
        return userService.updateUserRole(id, new UpdateUserRoleRequest(role));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto updateUserStatus(@Argument UUID id,
                                     @Argument Boolean isActive) {
        log.info("GQL updateUserStatus(id={}, active={})", id, isActive);
        return userService.updateUserStatus(id, new UserStatusRequest(isActive));
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> bulkUpdateUsers(@Argument BulkUserUpdateInput input) {
        log.info("GQL bulkUpdateUsers(userIds={})", input.getUserIds());
        BulkUserUpdateRequest request = BulkUserUpdateRequest.builder()
                .userIds(input.getUserIds())
                .role(input.getRole())
                .isActive(input.getIsActive())
                .sendNotification(input.getSendNotification() != null ? input.getSendNotification() : true)
                .build();
        return userService.bulkUpdateUsers(request);
    }

    // =========================================================================
    // PRODUCT MANAGEMENT MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ProductResponse adminUpdateProductInventory(@Argument UUID id,
                                                        @Argument Integer stock) {
        log.info("GQL adminUpdateProductInventory(id={}, stock={})", id, stock);
        var updateRequest = new ecommerce.modules.product.dto.UpdateProductRequest();
        updateRequest.setStock(stock);
        return productService.update(id, updateRequest);
    }

    // =========================================================================
    // SELLER MANAGEMENT MUTATIONS
    // =========================================================================

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto approveSeller(@Argument UUID id) {
        log.info("GQL approveSeller(id={})", id);
        return userService.approveSeller(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto suspendSeller(@Argument UUID id) {
        log.info("GQL suspendSeller(id={})", id);
        return userService.suspendSeller(id);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserDto reactivateSeller(@Argument UUID id) {
        log.info("GQL reactivateSeller(id={})", id);
        return userService.reactivateSeller(id);
    }

    // =========================================================================
    // HELPER METHODS
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

    private Predicate buildPredicateFromFilter(UserFilterInput filter) {
        return UserPredicates.builder()
                .withSearch(filter.getSearch())
                .withRole(filter.getRole())
                .withActive(filter.getActive())
                .withEmailVerified(filter.getEmailVerified())
                .withCreatedAfter(filter.getCreatedAfter())
                .withCreatedBefore(filter.getCreatedBefore())
                .withPhoneNumberContaining(filter.getPhoneNumber())
                .withNameContaining(filter.getName())
                .build();
    }
}
