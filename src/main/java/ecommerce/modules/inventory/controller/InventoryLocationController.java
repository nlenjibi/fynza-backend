package ecommerce.modules.inventory.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.inventory.dto.request.CreateInventoryLocationRequest;
import ecommerce.modules.inventory.dto.response.InventoryLocationResponse;
import ecommerce.modules.inventory.service.InventoryLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/inventory-locations")
@RequiredArgsConstructor
@Tag(name = "Inventory — Locations", description = "Inventory location mutations")
public class InventoryLocationController {

    private final InventoryLocationService locationService;

    @PostMapping
    @PreAuthorize("hasAuthority('inventory.location.create')")
    @Operation(summary = "Create an inventory location")
    public ResponseEntity<ApiResponse<InventoryLocationResponse>> createLocation(
            @Valid @RequestBody CreateInventoryLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Location created",
                        locationService.createLocation(request, principal.getId())));
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.location.update')")
    @Operation(summary = "Update an inventory location")
    public ResponseEntity<ApiResponse<InventoryLocationResponse>> updateLocation(
            @PathVariable UUID publicId,
            @Valid @RequestBody CreateInventoryLocationRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Location updated",
                locationService.updateLocation(publicId, request, principal.getId())));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.location.delete')")
    @Operation(summary = "Deactivate an inventory location")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        locationService.deleteLocation(publicId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Location deactivated", null));
    }
}
