package ecommerce.modules.product.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.product.dto.request.ProductStatusRequest;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.service.ProductStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Products", description = "Admin product lifecycle mutations")
public class AdminProductController {

    private final ProductStatusService statusService;

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a product listing (PENDING_REVIEW → ACTIVE)")
    public ResponseEntity<ApiResponse<ProductResponse>> approve(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product approved",
                statusService.approveProduct(principal.getId(), id)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a product listing")
    public ResponseEntity<ApiResponse<ProductResponse>> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) ProductStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success("Product rejected",
                statusService.rejectProduct(principal.getId(), id, reason)));
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a product")
    public ResponseEntity<ApiResponse<ProductResponse>> suspend(
            @PathVariable UUID id,
            @RequestBody(required = false) ProductStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = request != null ? request.getReason() : null;
        return ResponseEntity.ok(ApiResponse.success("Product suspended",
                statusService.suspendProduct(principal.getId(), id, reason)));
    }

    @PostMapping("/{id}/restore")
    @Operation(summary = "Restore a suspended product")
    public ResponseEntity<ApiResponse<ProductResponse>> restore(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product restored",
                statusService.restoreProduct(principal.getId(), id)));
    }
}
