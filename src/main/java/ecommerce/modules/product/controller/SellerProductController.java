package ecommerce.modules.product.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.product.dto.request.CreateProductRequest;
import ecommerce.modules.product.dto.request.CreateVariantRequest;
import ecommerce.modules.product.dto.request.UpdateProductRequest;
import ecommerce.modules.product.dto.request.UpdateVariantRequest;
import ecommerce.modules.product.dto.response.ProductResponse;
import ecommerce.modules.product.dto.response.ProductVariantResponse;
import ecommerce.modules.product.service.ProductService;
import ecommerce.modules.product.service.ProductStatusService;
import ecommerce.modules.product.service.ProductVariantService;
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
@RequestMapping("/v1/sellers/me/stores/{storeId}/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller — Products", description = "Seller product and variant mutations")
public class SellerProductController {

    private final ProductService        productService;
    private final ProductStatusService  statusService;
    private final ProductVariantService variantService;

    // ── Product mutations ────────────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a product in the specified store")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created",
                        productService.createProduct(principal.getId(), storeId, request)));
    }

    @PatchMapping("/{productId}")
    @Operation(summary = "Update product information")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product updated",
                productService.updateProduct(principal.getId(), productId, request)));
    }

    @PostMapping("/{productId}/submit")
    @Operation(summary = "Submit product for admin review")
    public ResponseEntity<ApiResponse<ProductResponse>> submitForReview(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product submitted for review",
                statusService.submitForReview(principal.getId(), productId)));
    }

    @PostMapping("/{productId}/publish")
    @Operation(summary = "Publish a reviewed product (make it ACTIVE)")
    public ResponseEntity<ApiResponse<ProductResponse>> publish(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product published",
                statusService.publishProduct(principal.getId(), productId)));
    }

    @PostMapping("/{productId}/deactivate")
    @Operation(summary = "Deactivate a product (ACTIVE → INACTIVE)")
    public ResponseEntity<ApiResponse<ProductResponse>> deactivate(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product deactivated",
                statusService.deactivateProduct(principal.getId(), productId)));
    }

    @PostMapping("/{productId}/archive")
    @Operation(summary = "Archive a product")
    public ResponseEntity<ApiResponse<ProductResponse>> archive(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product archived",
                productService.archiveProduct(principal.getId(), productId)));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Soft-delete a product")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable UUID productId,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.deleteProduct(principal.getId(), productId);
        return ResponseEntity.ok(ApiResponse.success("Product deleted", null));
    }

    // ── Variant mutations ────────────────────────────────────────────────────

    @PostMapping("/{productId}/variants")
    @Operation(summary = "Add a variant to a product")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> createVariant(
            @PathVariable UUID productId,
            @Valid @RequestBody CreateVariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variant created",
                        variantService.createVariant(principal.getId(), productId, request)));
    }

    @PatchMapping("/{productId}/variants/{variantId}")
    @Operation(summary = "Update a product variant")
    public ResponseEntity<ApiResponse<ProductVariantResponse>> updateVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @Valid @RequestBody UpdateVariantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Variant updated",
                variantService.updateVariant(principal.getId(), productId, variantId, request)));
    }

    @DeleteMapping("/{productId}/variants/{variantId}")
    @Operation(summary = "Delete a product variant")
    public ResponseEntity<ApiResponse<Void>> deleteVariant(
            @PathVariable UUID productId,
            @PathVariable UUID variantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        variantService.deleteVariant(principal.getId(), productId, variantId);
        return ResponseEntity.ok(ApiResponse.success("Variant deleted", null));
    }
}
