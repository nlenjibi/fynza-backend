package ecommerce.modules.user.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.user.dto.AddressDto;
import ecommerce.modules.user.dto.AddressRequest;
import ecommerce.modules.user.service.AddressService;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users/addresses")
@RequiredArgsConstructor
@Tag(name = "Address Management", description = "User address management endpoints")
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get all addresses", description = "Retrieve all addresses for the authenticated user")
    public ResponseEntity<ApiResponse<List<AddressDto>>> getAllAddresses(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID userId = principal.getId();
        List<AddressDto> addresses = addressService.getAddressesByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get address by ID", description = "Retrieve a specific address by ID")
    public ResponseEntity<ApiResponse<AddressDto>> getAddressById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {
        UUID userId = principal.getId();
        AddressDto address = addressService.getAddressById(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address retrieved successfully", address));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create new address", description = "Create a new address for the authenticated user")
    public ResponseEntity<ApiResponse<AddressDto>> createAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddressRequest request) {
        UUID userId = principal.getId();
        AddressDto createdAddress = addressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address created successfully", createdAddress));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update address", description = "Update an existing address")
    public ResponseEntity<ApiResponse<AddressDto>> updateAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        UUID userId = principal.getId();
        AddressDto updatedAddress = addressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", updatedAddress));
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Delete address", description = "Delete an existing address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {
        UUID userId = principal.getId();
        addressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PutMapping("/{addressId}/default")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Set default address", description = "Set an address as the default address")
    public ResponseEntity<ApiResponse<AddressDto>> setDefaultAddress(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID addressId) {
        UUID userId = principal.getId();
        AddressDto updatedAddress = addressService.setDefaultAddress(userId, addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated successfully", updatedAddress));
    }
}
