package ecommerce.modules.refund.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.refund.dto.CreateReturnPolicyRequest;
import ecommerce.modules.refund.dto.ReturnPolicyResponse;
import ecommerce.modules.refund.service.ReturnPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/return-policies")
@RequiredArgsConstructor
@Tag(name = "Admin Return Policies", description = "Return policy management")
public class AdminReturnPolicyController {

    private final ReturnPolicyService returnPolicyService;

    @PostMapping
    @PreAuthorize("hasAuthority('return:policy.manage')")
    @Operation(summary = "Create return policy")
    public ResponseEntity<ApiResponse<ReturnPolicyResponse>> createPolicy(
            @Valid @RequestBody CreateReturnPolicyRequest request) {
        ReturnPolicyResponse response = returnPolicyService.createPolicy(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return policy created", response));
    }

    @PostMapping("/{policyId}")
    @PreAuthorize("hasAuthority('return:policy.manage')")
    @Operation(summary = "Update return policy")
    public ResponseEntity<ApiResponse<ReturnPolicyResponse>> updatePolicy(
            @PathVariable UUID policyId,
            @Valid @RequestBody CreateReturnPolicyRequest request) {
        ReturnPolicyResponse response = returnPolicyService.updatePolicy(policyId, request);
        return ResponseEntity.ok(ApiResponse.success("Return policy updated", response));
    }

    @PostMapping("/{policyId}/toggle")
    @PreAuthorize("hasAuthority('return:policy.manage')")
    @Operation(summary = "Toggle policy active state")
    public ResponseEntity<ApiResponse<ReturnPolicyResponse>> togglePolicy(@PathVariable UUID policyId) {
        ReturnPolicyResponse response = returnPolicyService.togglePolicy(policyId);
        return ResponseEntity.ok(ApiResponse.success("Return policy toggled", response));
    }
}
