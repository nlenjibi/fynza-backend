package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.dto.request.PackageRequest;
import ecommerce.modules.shipping.dto.response.PackageResponse;
import ecommerce.modules.shipping.service.PackageService;
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
@RequestMapping("/v1/seller/shipping/shipments/{shipmentId}/packages")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Shipment Packages", description = "Manage packages within a shipment")
public class PackageController {

    private final PackageService packageService;

    @PostMapping
    @Operation(summary = "Add a package to a shipment")
    public ResponseEntity<ApiResponse<PackageResponse>> addPackage(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody PackageRequest request) {
        PackageResponse response = packageService.addPackage(shipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Package added", response));
    }

    @DeleteMapping("/{packageId}")
    @Operation(summary = "Remove a package from a shipment")
    public ResponseEntity<ApiResponse<Void>> removePackage(
            @PathVariable UUID shipmentId,
            @PathVariable UUID packageId) {
        packageService.removePackage(packageId);
        return ResponseEntity.ok(ApiResponse.success("Package removed", null));
    }
}
