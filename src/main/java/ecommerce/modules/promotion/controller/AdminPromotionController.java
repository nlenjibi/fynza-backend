package ecommerce.modules.promotion.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.promotion.entity.AdminPromotion;
import ecommerce.modules.promotion.service.AdminPromotionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPromotionController {

    private final AdminPromotionService adminPromotionService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminPromotion>> createPromotion(
            @RequestParam UUID adminId,
            @RequestParam String name,
            @RequestParam AdminPromotion.PromotionType type,
            @RequestParam(required = false) String code,
            @RequestParam BigDecimal discountValue,
            @RequestParam(required = false) BigDecimal minPurchase,
            @RequestParam(required = false) BigDecimal maxDiscount,
            @RequestParam LocalDateTime startDate,
            @RequestParam LocalDateTime endDate,
            @RequestParam(required = false) Integer usageLimit,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false, defaultValue = "false") Boolean isGlobal,
            @RequestParam(required = false, defaultValue = "0.0.0.0") String ipAddress) {
        
        AdminPromotion promotion = adminPromotionService.createPromotion(
                adminId, name, type, code, discountValue, minPurchase, maxDiscount,
                startDate, endDate, usageLimit, categoryId, isGlobal, ipAddress);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Promotion created successfully", promotion));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminPromotion>>> getPromotions(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                adminPromotionService.getPromotions(pageable)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AdminPromotion>>> getActivePromotions() {
        return ResponseEntity.ok(ApiResponse.success(
                adminPromotionService.getActivePromotions()));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "activeCount", adminPromotionService.countActivePromotions(),
                "totalRevenue", adminPromotionService.getTotalRevenue()
        )));
    }

    @DeleteMapping("/{promotionId}")
    public ResponseEntity<ApiResponse<Void>> deletePromotion(
            @RequestParam UUID adminId,
            @PathVariable UUID promotionId,
            @RequestParam(required = false, defaultValue = "0.0.0.0") String ipAddress) {
        adminPromotionService.deletePromotion(adminId, promotionId, ipAddress);
        return ResponseEntity.ok(ApiResponse.success("Promotion deleted successfully", null));
    }
}
