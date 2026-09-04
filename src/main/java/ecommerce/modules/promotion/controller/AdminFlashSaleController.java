package ecommerce.modules.promotion.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.promotion.entity.AdminFlashSale;
import ecommerce.modules.promotion.entity.AdminFlashSale.Status;
import ecommerce.modules.promotion.service.AdminFlashSaleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/flash-sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFlashSaleController {

    private final AdminFlashSaleService adminFlashSaleService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminFlashSale>> createFlashSale(
            @RequestParam UUID createdBy,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Integer discountPercent,
            @RequestParam(required = false) BigDecimal minPurchaseAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) Integer maxProductsPerSeller,
            @RequestParam(required = false) Integer maxTotalProducts,
            @RequestParam(required = false) String category,
            @RequestParam LocalDateTime startDatetime,
            @RequestParam LocalDateTime endDatetime) {

        AdminFlashSale flashSale = adminFlashSaleService.createFlashSale(
                createdBy, name, description, discountPercent, minPurchaseAmount,
                maxDiscountAmount, maxProductsPerSeller, maxTotalProducts, category,
                startDatetime, endDatetime);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Flash sale created successfully", flashSale));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminFlashSale>>> getAllFlashSales(
            @RequestParam(required = false) Status status,
            Pageable pageable) {
        Page<AdminFlashSale> flashSales = (status != null) 
                ? adminFlashSaleService.getFlashSalesByStatus(status, pageable)
                : adminFlashSaleService.getAllFlashSales(pageable);
        return ResponseEntity.ok(ApiResponse.success(flashSales));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminFlashSale>> getFlashSale(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminFlashSaleService.getFlashSale(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminFlashSale>> updateFlashSale(
            @PathVariable UUID id,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer discountPercent,
            @RequestParam(required = false) BigDecimal minPurchaseAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) LocalDateTime startDatetime,
            @RequestParam(required = false) LocalDateTime endDatetime) {
        return ResponseEntity.ok(ApiResponse.success(
                "Flash sale updated successfully",
                adminFlashSaleService.updateFlashSale(id, name, description, discountPercent,
                        minPurchaseAmount, maxDiscountAmount, startDatetime, endDatetime)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFlashSale(@PathVariable UUID id) {
        adminFlashSaleService.deleteFlashSale(id);
        return ResponseEntity.ok(ApiResponse.success("Flash sale deleted successfully", null));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "activeCount", adminFlashSaleService.countActive()
        )));
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<ApiResponse<Long>> getSlotsRemaining(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(adminFlashSaleService.getSlotsRemaining(id)));
    }
}
