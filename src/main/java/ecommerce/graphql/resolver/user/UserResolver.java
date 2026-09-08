package ecommerce.graphql.resolver.user;

import ecommerce.common.response.PaginatedResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.graphql.dto.UserProfilePageDto;
import ecommerce.graphql.input.PageInput;
import ecommerce.graphql.input.SortDirection;
import ecommerce.graphql.input.UserFilterInput;
import ecommerce.modules.user.dto.AddressDto;
import ecommerce.modules.user.dto.AdminUserSearchParams;
import ecommerce.modules.user.dto.CustomerDashboardResponse;
import ecommerce.modules.user.dto.LoyaltyRedemptionResponse;
import ecommerce.modules.user.dto.UserDto;
import ecommerce.modules.user.dto.UserProfileResponse;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.service.UserService;
import ecommerce.modules.user.spec.UserSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    // ── Self-service queries ──────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserProfileResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL me(user={})", principal.getId());
        return userService.getMyProfile(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public UserDto currentUser(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL currentUser(user={})", principal.getId());
        return userService.getUserById(principal.getId()).orElse(null);
    }

    // ── Admin queries ─────────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfileResponse user(@Argument UUID id) {
        log.debug("GQL user(id={})", id);
        return userService.adminGetUser(id);
    }

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    public UserProfilePageDto users(@Argument PageInput pagination, @Argument UserFilterInput filter) {
        log.debug("GQL users");
        Pageable pageable = toPageable(pagination);

        AdminUserSearchParams params = new AdminUserSearchParams();
        if (filter != null) {
            params.setQuery(filter.getSearch() != null ? filter.getSearch() : filter.getName());
            params.setRole(filter.getRole());
            params.setEmailVerified(filter.getEmailVerified());
        }

        Page<UserProfileResponse> page = userService.adminSearchUsers(params, pageable);
        return UserProfilePageDto.builder()
                .content(page.getContent())
                .pageInfo(PaginatedResponse.from(page))
                .build();
    }

    // ── Customer queries ──────────────────────────────────────────────────────

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerDashboardResponse customerDashboard(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL customerDashboard(user={})", principal.getId());
        return userService.getCustomerDashboard(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<AddressDto> myAddresses(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL myAddresses(user={})", principal.getId());
        return userService.getCustomerAddresses(principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public LoyaltyRedemptionResponse loyaltyBalance(@AuthenticationPrincipal UserPrincipal principal) {
        log.debug("GQL loyaltyBalance(user={})", principal.getId());
        return userService.getLoyaltyBalance(principal.getId());
    }

    // ── Field resolvers ───────────────────────────────────────────────────────

    @SchemaMapping(typeName = "User")
    public String fullName(UserDto user) {
        return user.getFullName();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Pageable toPageable(PageInput input) {
        if (input == null) return PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Sort sort = input.getDirection() == SortDirection.DESC
                ? Sort.by(input.getSortBy()).descending()
                : Sort.by(input.getSortBy()).ascending();
        return PageRequest.of(input.getPage(), input.getSize(), sort);
    }

    @SuppressWarnings("unused")
    private Specification<User> buildPredicateFromFilter(UserFilterInput filter) {
        return Specification.where(UserSpec.emailOrNameContains(filter.getSearch()))
                .and(filter.getRole() != null ? UserSpec.hasRole(filter.getRole()) : null)
                .and(Boolean.TRUE.equals(filter.getActive()) ? UserSpec.isActive() : null);
    }
}
