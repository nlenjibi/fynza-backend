package ecommerce.modules.customer.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.customer.dto.request.CustomerAddressRequest;
import ecommerce.modules.customer.dto.request.CustomerPreferenceRequest;
import ecommerce.modules.customer.dto.request.CustomerUpdateRequest;
import ecommerce.modules.customer.dto.response.CustomerAddressResponse;
import ecommerce.modules.customer.dto.response.CustomerPreferenceResponse;
import ecommerce.modules.customer.dto.response.CustomerResponse;
import ecommerce.modules.customer.service.CustomerAddressService;
import ecommerce.modules.customer.service.CustomerPreferenceService;
import ecommerce.modules.customer.service.CustomerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customers/me")
@RequiredArgsConstructor
public class CustomerMeController {

    private final CustomerService           customerService;
    private final CustomerAddressService    addressService;
    private final CustomerPreferenceService preferenceService;

    // ── Profile mutations ─────────────────────────────────────────────────────

    @PatchMapping
    @PreAuthorize("hasAuthority('customer.update.own')")
    public ResponseEntity<ApiResponse<CustomerResponse>> updateMyCustomer(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Customer profile updated",
                customerService.updateMyCustomer(principal.getId(), request)));
    }

    // ── Preference mutations ───────────────────────────────────────────────────

    @PatchMapping("/preferences")
    @PreAuthorize("hasAuthority('customer.update.own')")
    public ResponseEntity<ApiResponse<CustomerPreferenceResponse>> updatePreferences(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerPreferenceRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Preferences updated",
                preferenceService.updatePreferences(principal.getId(), request)));
    }

    // ── Address mutations ─────────────────────────────────────────────────────

    @PostMapping("/addresses")
    @PreAuthorize("hasAuthority('address.create.own')")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> addAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added",
                        addressService.addAddress(principal.getId(), request)));
    }

    @PatchMapping("/addresses/{addressId}")
    @PreAuthorize("hasAuthority('address.update.own')")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody CustomerAddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address updated",
                addressService.updateAddress(principal.getId(), addressId, request)));
    }

    @DeleteMapping("/addresses/{addressId}")
    @PreAuthorize("hasAuthority('address.delete.own')")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {
        addressService.deleteAddress(principal.getId(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted", null));
    }

    @PostMapping("/addresses/{addressId}/default")
    @PreAuthorize("hasAuthority('address.update.own')")
    public ResponseEntity<ApiResponse<CustomerAddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {
        return ResponseEntity.ok(ApiResponse.success("Default address set",
                addressService.setDefaultAddress(principal.getId(), addressId)));
    }
}
