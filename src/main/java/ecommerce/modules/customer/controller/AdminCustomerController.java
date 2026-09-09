package ecommerce.modules.customer.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.customer.dto.request.CustomerStatusRequest;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.service.CustomerStatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerStatusService customerStatusService;

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('customer.suspend')")
    public ResponseEntity<ApiResponse<CustomerResponse>> suspendCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer suspended",
                customerStatusService.suspendCustomer(id, principal.getId(), request)));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('customer.activate')")
    public ResponseEntity<ApiResponse<CustomerResponse>> activateCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Customer activated",
                customerStatusService.activateCustomer(id, principal.getId())));
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasAuthority('customer.block')")
    public ResponseEntity<ApiResponse<CustomerResponse>> blockCustomer(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer blocked",
                customerStatusService.blockCustomer(id, principal.getId(), request)));
    }
}
