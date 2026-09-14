package ecommerce.modules.media.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.media.dto.request.AttachMediaRequest;
import ecommerce.modules.media.dto.request.ReorderMediaRequest;
import ecommerce.modules.media.dto.response.ProductMediaResponse;
import ecommerce.modules.media.service.ProductMediaService;
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
@RequestMapping("/v1/products/{productId}/media")
@RequiredArgsConstructor
@Tag(name = "Product Media", description = "Product media attachment mutations")
public class ProductMediaController {

    private final ProductMediaService productMediaService;

    @PostMapping
    @PreAuthorize("hasAuthority('media.product.attach')")
    @Operation(summary = "Attach a media asset to a product")
    public ResponseEntity<ApiResponse<ProductMediaResponse>> attachMedia(
            @PathVariable UUID productId,
            @Valid @RequestBody AttachMediaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media attached to product",
                        productMediaService.attachMedia(productId, request, principal.getId())));
    }

    @DeleteMapping("/{mediaAssetPublicId}")
    @PreAuthorize("hasAuthority('media.product.detach')")
    @Operation(summary = "Detach a media asset from a product")
    public ResponseEntity<ApiResponse<Void>> detachMedia(
            @PathVariable UUID productId,
            @PathVariable UUID mediaAssetPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        productMediaService.detachMedia(productId, mediaAssetPublicId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Media detached from product", null));
    }

    @PutMapping("/reorder")
    @PreAuthorize("hasAuthority('media.product.reorder')")
    @Operation(summary = "Reorder media assets for a product")
    public ResponseEntity<ApiResponse<Void>> reorderMedia(
            @PathVariable UUID productId,
            @Valid @RequestBody ReorderMediaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        productMediaService.reorderMedia(productId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Media reordered", null));
    }
}
