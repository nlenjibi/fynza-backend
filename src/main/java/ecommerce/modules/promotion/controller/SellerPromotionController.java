package ecommerce.modules.promotion.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.promotion.dto.SellerPromotionDto;
import ecommerce.modules.promotion.service.SellerPromotionService;
import ecommerce.common.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/sellers/promotions")
@RequiredArgsConstructor
@Tag(name = "Seller Promotions", description = "Seller promotion management endpoints")
@PreAuthorize("hasRole('SELLER')")
public class SellerPromotionController {

    private final SellerPromotionService sellerPromotionService;

    @PostMapping
    @Operation(summary = "Create promotion", description = "Create a new promotion for the authenticated seller")
    public ResponseEntity<ApiResponse<SellerPromotionDto>> createPromotion(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody CreatePromotionRequest request) {
        
        UUID sellerId = UUID.fromString(principal.getId().toString());
        SellerPromotionDto promotion = sellerPromotionService.createPromotion(
                sellerId, request.getName(), request.getType(), request.getDiscountValue(), 
                request.getMinPurchase(), request.getStartDate(), request.getEndDate(), 
                request.getUsageLimit(), request.getIpAddress());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion created successfully", promotion));
    }

    @GetMapping
    @Operation(summary = "Get seller promotions", description = "Get all promotions for the authenticated seller")
    public ResponseEntity<ApiResponse<Page<SellerPromotionDto>>> getPromotions(
            @AuthenticationPrincipal UserPrincipal principal,
            Pageable pageable) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success(
                sellerPromotionService.getPromotions(sellerId, pageable)));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active promotions", description = "Get active promotions for the authenticated seller")
    public ResponseEntity<ApiResponse<List<SellerPromotionDto>>> getActivePromotions(
            @AuthenticationPrincipal UserPrincipal principal) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        return ResponseEntity.ok(ApiResponse.success(
                sellerPromotionService.getActivePromotions(sellerId)));
    }

    @DeleteMapping("/{promotionId}")
    @Operation(summary = "Delete promotion", description = "Delete a promotion for the authenticated seller")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID promotionId) {
        UUID sellerId = UUID.fromString(principal.getId().toString());
        sellerPromotionService.deletePromotion(sellerId, promotionId, null);
        return ResponseEntity.ok(ApiResponse.success("Promotion deleted successfully", null));
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CreatePromotionRequest {
        private String name;
        private String type;
        private BigDecimal discountValue;
        private BigDecimal minPurchase;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Integer usageLimit;
        private String ipAddress;
    }
}
