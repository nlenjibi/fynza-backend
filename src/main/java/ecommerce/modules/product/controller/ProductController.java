package ecommerce.modules.product.controller;

import ecommerce.common.enums.PaymentMethod;
import ecommerce.common.enums.Role;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.product.dto.CreateProductRequest;
import ecommerce.modules.product.dto.ProductFilterRequest;
import ecommerce.modules.product.dto.ProductResponse;
import ecommerce.modules.product.dto.UpdateProductRequest;
import ecommerce.modules.product.service.ProductService;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/v1/products")
@RequiredArgsConstructor
@Tag(name = "Product Management", description = "APIs for managing products")
public class ProductController {

    private final ProductService productService;

    @GetMapping
    @Operation(summary = "Get all products (paginated)", description = "Public endpoint - returns paginated products")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getAllProducts(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status) {
        ProductFilterRequest filter = ProductFilterRequest.builder()
                .categoryId(categoryId)
                .search(search)
                .status(status)
                .build();
        Page<ProductResponse> products = productService.findAll(filter, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(products)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Public endpoint - returns product detail")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        ProductResponse product = productService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Create product", description = "Authenticated SELLER only")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal,
            UriComponentsBuilder uriBuilder) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        ProductResponse response = productService.create(request, sellerId);
        var uri = uriBuilder.path("/api/v1/products/{id}").buildAndExpand(response.getId()).toUri();
        return ResponseEntity.created(uri)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Update product", description = "Authenticated SELLER (owner) or ADMIN")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!productService.canUpdate(id.toString(), principal.getId().toString())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to update this product"));
        }
        
        ProductResponse response = productService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Delete product", description = "Authenticated SELLER (owner) or ADMIN")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (!productService.canDelete(id.toString(), principal.getId().toString())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to delete this product"));
        }
        
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @GetMapping("/seller/products")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Get seller's products", description = "Returns products for the authenticated seller")
    public ResponseEntity<ApiResponse<PaginatedResponse<ProductResponse>>> getSellerProducts(
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        Page<ProductResponse> products = productService.findBySellerId(sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(PaginatedResponse.from(products)));
    }

    @PostMapping("/{id}/stock/add")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Add stock to product", description = "Add inventory to a product")
    public ResponseEntity<ApiResponse<ProductResponse>> addStock(
            @PathVariable UUID id,
            @RequestParam int quantity,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existingProduct = productService.findById(id);
        boolean isOwner = existingProduct.getSeller() != null && 
                existingProduct.getSeller().getId().toString().equals(principal.getId().toString());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to update this product"));
        }
        
        ProductResponse response = productService.addStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock added successfully", response));
    }

    @PostMapping("/{id}/stock/reduce")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Reduce stock from product", description = "Reduce inventory from a product")
    public ResponseEntity<ApiResponse<ProductResponse>> reduceStock(
            @PathVariable UUID id,
            @RequestParam int quantity,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existingProduct = productService.findById(id);
        boolean isOwner = existingProduct.getSeller() != null && 
                existingProduct.getSeller().getId().toString().equals(principal.getId().toString());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to update this product"));
        }
        
        ProductResponse response = productService.reduceStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reduced successfully", response));
    }

    @PostMapping("/{id}/stock/reserve")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Reserve stock for product", description = "Reserve inventory for a product (for order processing)")
    public ResponseEntity<ApiResponse<ProductResponse>> reserveStock(
            @PathVariable UUID id,
            @RequestParam int quantity) {
        ProductResponse response = productService.reserveStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock reserved successfully", response));
    }

    @PostMapping("/{id}/stock/release")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Release reserved stock", description = "Release previously reserved inventory")
    public ResponseEntity<ApiResponse<ProductResponse>> releaseReservedStock(
            @PathVariable UUID id,
            @RequestParam int quantity) {
        ProductResponse response = productService.releaseReservedStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Reserved stock released successfully", response));
    }

    @PostMapping("/{id}/stock/restore")
    @PreAuthorize("hasRole('SELLER') or hasRole('ADMIN')")
    @Operation(summary = "Restore stock", description = "Restore inventory (e.g., after order cancellation)")
    public ResponseEntity<ApiResponse<ProductResponse>> restoreStock(
            @PathVariable UUID id,
            @RequestParam int quantity,
            @AuthenticationPrincipal UserPrincipal principal) {
        ProductResponse existingProduct = productService.findById(id);
        boolean isOwner = existingProduct.getSeller() != null && 
                existingProduct.getSeller().getId().toString().equals(principal.getId().toString());
        boolean isAdmin = principal.getRole() == Role.ADMIN;
        
        if (!isOwner && !isAdmin) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("You are not authorized to update this product"));
        }
        
        ProductResponse response = productService.restoreStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock restored successfully", response));
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "Increment product view count", description = "Increment the view count for a product")
    public ResponseEntity<ApiResponse<Boolean>> incrementViewCount(@PathVariable UUID id) {
        Boolean result = productService.incrementViewCount(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/rating")
    @Operation(summary = "Update product rating", description = "Update the rating for a product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateRating(
            @PathVariable UUID id,
            @RequestParam Float rating) {
        ProductResponse response = productService.updateRating(id, rating);
        return ResponseEntity.ok(ApiResponse.success("Rating updated successfully", response));
    }
}
