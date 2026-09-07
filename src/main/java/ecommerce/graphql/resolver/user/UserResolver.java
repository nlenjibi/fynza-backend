package ecommerce.graphql.resolver.user;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.user.dto.*;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.spec.UserSpec;
import ecommerce.modules.user.service.UserService;
import org.springframework.data.jpa.domain.Specification;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.graphql.dto.UserResponceDto;
import ecommerce.graphql.input.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class UserResolver {

    private final UserService userService;

    // =========================================================================
    // PUBLIC / AUTHENTICATED QUERIES
    // =========================================================================

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserDto user(@Argument UUID id) {
        log.info("GQL user(id={})", id);
        return userService.getUserById(id).orElse(null);
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
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
        return userService.getCustomerDashboard(principal.getId());
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
        return userService.getLoyaltyBalance(principal.getId());
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
