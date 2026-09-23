package ecommerce.graphql.resolver.returns;

import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.refund.dto.ReturnAuditResponse;
import ecommerce.modules.refund.dto.ReturnDispositionResponse;
import ecommerce.modules.refund.dto.ReturnEligibilityResult;
import ecommerce.modules.refund.dto.ReturnEvidenceResponse;
import ecommerce.modules.refund.dto.ReturnInspectionResponse;
import ecommerce.modules.refund.dto.ReturnPolicyResponse;
import ecommerce.modules.refund.dto.ReturnReconciliationResponse;
import ecommerce.modules.refund.dto.ReturnRefundResponse;
import ecommerce.modules.refund.dto.ReturnResponse;
import ecommerce.modules.refund.service.ReturnEvidenceService;
import ecommerce.modules.refund.service.ReturnInspectionService;
import ecommerce.modules.refund.service.ReturnPolicyService;
import ecommerce.modules.refund.service.ReturnRefundService;
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
    private final ReturnEvidenceService returnEvidenceService;
    private final ReturnInspectionService returnInspectionService;
    private final ReturnRefundService returnRefundService;

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
    @PreAuthorize("isAuthenticated()")
    public List<ReturnEvidenceResponse> returnEvidence(@Argument String returnId) {
        return returnEvidenceService.getEvidence(UUID.fromString(returnId));
    }

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    public List<ReturnAuditResponse> returnAuditTrail(@Argument String returnId) {
        return returnEvidenceService.getAuditTrail(UUID.fromString(returnId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:inspect')")
    public List<ReturnInspectionResponse> returnInspections(@Argument String returnId) {
        return returnInspectionService.getInspections(UUID.fromString(returnId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:inspect')")
    public List<ReturnDispositionResponse> returnDispositions(@Argument String returnId) {
        return returnInspectionService.getDispositions(UUID.fromString(returnId));
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:read')")
    public ReturnRefundResponse returnRefund(@Argument String returnId) {
        return returnRefundService.getRefund(UUID.fromString(returnId)).orElse(null);
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('return:admin.read')")
    public List<ReturnReconciliationResponse> returnReconciliations(@Argument String returnId) {
        return returnRefundService.getReconciliations(UUID.fromString(returnId));
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
