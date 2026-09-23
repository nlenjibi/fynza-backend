package ecommerce.graphql.resolver.returns;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.refund.dto.ReturnEligibilityResult;
import ecommerce.modules.refund.dto.ReturnPolicyResponse;
import ecommerce.modules.refund.dto.ReturnResponse;
import ecommerce.modules.refund.service.ReturnPolicyService;
import ecommerce.modules.refund.service.ReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
public class ReturnQueryResolver {

    private final ReturnService returnService;
    private final ReturnPolicyService returnPolicyService;

    @QueryMapping
    @PreAuthorize("hasAuthority('return:read')")
    public Page<ReturnResponse> myReturns(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument int page,
            @Argument int size) {
        return returnService.getReturnsByCustomer(
                principal.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:read')")
    public ReturnResponse myReturn(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String returnId) {
        return returnService.getReturn(UUID.fromString(returnId), principal.getId());
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:read')")
    public ReturnEligibilityResult returnEligibility(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument String orderId) {
        return returnService.checkEligibility(UUID.fromString(orderId), principal.getId());
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<ReturnResponse> orderReturns(@Argument String orderId) {
        return returnService.getReturnsByOrder(UUID.fromString(orderId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:review')")
    public Page<ReturnResponse> sellerReturns(
            @AuthenticationPrincipal UserPrincipal principal,
            @Argument int page,
            @Argument int size) {
        return returnService.getReturnsBySeller(
                principal.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:admin.read')")
    public Page<ReturnResponse> adminReturns(
            @Argument int page,
            @Argument int size) {
        return returnService.getAllReturns(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @QueryMapping
    public ReturnPolicyResponse platformReturnPolicy() {
        return returnPolicyService.getPlatformPolicy();
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public ReturnPolicyResponse storeReturnPolicy(@Argument String storeId) {
        return returnPolicyService.getStorePolicy(UUID.fromString(storeId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:policy.read')")
    public ReturnPolicyResponse returnPolicy(@Argument String policyId) {
        return returnPolicyService.getPolicy(UUID.fromString(policyId));
    }
}
