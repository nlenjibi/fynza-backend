package ecommerce.modules.product.controller;

import ecommerce.common.enums.ProductStatus;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.product.dto.*;
import ecommerce.modules.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Seller Products", description = "Seller product management")
public class SellerProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get seller products")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(required = false) ProductStatus status,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully",
                productService.findBySellerId(principal.getId(), status, categoryId, search, pageable)));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get seller product stats")
    public ResponseEntity<ApiResponse<SellerProductStatsResponse>> getProductStats(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Product stats retrieved successfully",
                productService.getSellerProductStats(principal.getId())));
    }

    @PostMapping
    @Operation(summary = "Create product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully",
                        productService.create(request, principal.getId())));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existing = productService.findById(id);
        boolean isOwner = existing.getSeller() != null &&
                existing.getSeller().getId().toString().equals(principal.getId().toString());
        if (!isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to update this product"));
        }
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existing = productService.findById(id);
        boolean isOwner = existing.getSeller() != null &&
                existing.getSeller().getId().toString().equals(principal.getId().toString());
        if (!isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to delete this product"));
        }
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @PostMapping("/{id}/tags")
    @Operation(summary = "Assign tags to product")
    public ResponseEntity<ApiResponse<Void>> assignTags(
            @PathVariable UUID id,
            @RequestBody List<String> tags,
            @AuthenticationPrincipal UserPrincipal principal) {
        productService.assignTagsToProduct(id, tags, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Tags assigned successfully", null));
    }
}
