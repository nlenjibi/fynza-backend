package ecommerce.modules.media.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.media.dto.request.AttachProductMediaRequest;
import ecommerce.modules.media.dto.request.CompleteUploadRequest;
import ecommerce.modules.media.dto.request.InitiateUploadRequest;
import ecommerce.modules.media.dto.response.MediaAssetResponse;
import ecommerce.modules.media.dto.response.SignedUrlResponse;
import ecommerce.modules.media.dto.response.UploadSessionResponse;
import ecommerce.modules.media.service.MediaService;
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
@RequestMapping("/v1/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media upload and management mutations")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping("/uploads")
    @PreAuthorize("hasAuthority('media.upload')")
    @Operation(summary = "Initiate a media upload session")
    public ResponseEntity<ApiResponse<UploadSessionResponse>> initiateUpload(
            @Valid @RequestBody InitiateUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Upload session created",
                        mediaService.initiateUpload(request, principal.getId())));
    }

    @PostMapping("/uploads/{uploadId}/complete")
    @PreAuthorize("hasAuthority('media.upload')")
    @Operation(summary = "Complete a media upload session")
    public ResponseEntity<ApiResponse<MediaAssetResponse>> completeUpload(
            @PathVariable UUID uploadId,
            @Valid @RequestBody CompleteUploadRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Upload completed",
                mediaService.completeUpload(uploadId, request, principal.getId())));
    }

    @PostMapping("/uploads/{uploadId}/cancel")
    @PreAuthorize("hasAuthority('media.session.cancel')")
    @Operation(summary = "Cancel an in-progress upload session")
    public ResponseEntity<ApiResponse<Void>> cancelUpload(
            @PathVariable UUID uploadId,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.cancelUpload(uploadId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Upload cancelled", null));
    }

    @DeleteMapping("/{publicId}")
    @PreAuthorize("hasAuthority('media.delete')")
    @Operation(summary = "Delete a media asset")
    public ResponseEntity<ApiResponse<Void>> deleteAsset(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.deleteAsset(publicId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Media asset deleted", null));
    }

    @PostMapping("/{publicId}/signed-url")
    @PreAuthorize("hasAuthority('media.url.read')")
    @Operation(summary = "Generate a signed download URL for a private media asset")
    public ResponseEntity<ApiResponse<SignedUrlResponse>> getSignedUrl(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Signed URL generated",
                mediaService.getSignedDownloadUrl(publicId, principal.getId())));
    }

    @PostMapping("/products/{productId}/media")
    @PreAuthorize("hasAuthority('media.product.attach')")
    @Operation(summary = "Attach a media asset to a product")
    public ResponseEntity<ApiResponse<Void>> attachToProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody AttachProductMediaRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.attachToProduct(productId, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Media attached to product", null));
    }

    @DeleteMapping("/products/{productId}/media/{mediaAssetPublicId}")
    @PreAuthorize("hasAuthority('media.product.detach')")
    @Operation(summary = "Detach a media asset from a product")
    public ResponseEntity<ApiResponse<Void>> detachFromProduct(
            @PathVariable UUID productId,
            @PathVariable UUID mediaAssetPublicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.detachFromProduct(productId, mediaAssetPublicId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Media detached from product", null));
    }

    @PatchMapping("/products/{productId}/media/reorder")
    @PreAuthorize("hasAuthority('media.product.reorder')")
    @Operation(summary = "Reorder media assets for a product")
    public ResponseEntity<ApiResponse<Void>> reorderProductMedia(
            @PathVariable UUID productId,
            @RequestBody List<UUID> orderedMediaIds,
            @AuthenticationPrincipal UserPrincipal principal) {
        mediaService.reorderProductMedia(productId, orderedMediaIds, principal.getId());
        return ResponseEntity.ok(ApiResponse.success("Media reordered", null));
    }
}
