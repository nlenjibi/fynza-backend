package ecommerce.modules.tag.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.tag.entity.Tag;
import ecommerce.modules.tag.service.SellerProductTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/tags")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerTagController {

    private final SellerProductTagService sellerProductTagService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<List<Tag>>> getAvailableTags(@RequestParam UUID sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                sellerProductTagService.getAvailableTags(sellerId)));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<List<Tag>>> getTagsForProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(
                sellerProductTagService.getTagsForProduct(productId)));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<List<Object[]>>> getTagStats(@RequestParam UUID sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                sellerProductTagService.getSellerTagStats(sellerId)));
    }

    @PostMapping("/assign")
    public ResponseEntity<ApiResponse<Void>> assignTagToProduct(
            @RequestParam UUID sellerId,
            @RequestParam UUID productId,
            @RequestParam UUID tagId,
            @RequestParam UUID userId,
            @RequestParam(required = false, defaultValue = "0.0.0.0") String ipAddress) {
        sellerProductTagService.assignTagToProduct(sellerId, productId, tagId, userId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Tag assigned successfully", null));
    }

    @PostMapping("/unassign")
    public ResponseEntity<ApiResponse<Void>> unassignTagFromProduct(
            @RequestParam UUID sellerId,
            @RequestParam UUID productId,
            @RequestParam UUID tagId,
            @RequestParam UUID userId,
            @RequestParam(required = false, defaultValue = "0.0.0.0") String ipAddress) {
        sellerProductTagService.unassignTagFromProduct(sellerId, productId, tagId, userId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Tag unassigned successfully", null));
    }

    @PostMapping("/set")
    public ResponseEntity<ApiResponse<Void>> setProductTags(
            @RequestParam UUID sellerId,
            @RequestParam UUID productId,
            @RequestParam List<UUID> tagIds,
            @RequestParam UUID userId,
            @RequestParam(required = false, defaultValue = "0.0.0.0") String ipAddress) {
        sellerProductTagService.setProductTags(sellerId, productId, tagIds, userId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Tags updated successfully", null));
    }
}
