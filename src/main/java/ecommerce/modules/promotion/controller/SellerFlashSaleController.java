package ecommerce.modules.promotion.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.promotion.entity.AdminFlashSale;
import ecommerce.modules.promotion.entity.SellerFlashSaleApplication;
import ecommerce.modules.promotion.service.AdminFlashSaleService;
import ecommerce.modules.promotion.service.SellerFlashSaleApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/flash-sales")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerFlashSaleController {

    private final AdminFlashSaleService adminFlashSaleService;
    private final SellerFlashSaleApplicationService applicationService;

    @GetMapping("/available")
    public ResponseEntity<ApiResponse<Page<AdminFlashSale>>> getAvailableFlashSales(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFlashSaleService.getFlashSalesByStatus(AdminFlashSale.Status.ACTIVE, pageable)));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<Page<AdminFlashSale>>> getUpcomingFlashSales(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                adminFlashSaleService.getFlashSalesByStatus(AdminFlashSale.Status.SCHEDULED, pageable)));
    }

    @PostMapping("/apply")
    public ResponseEntity<ApiResponse<SellerFlashSaleApplication>> applyToFlashSale(
            @RequestParam UUID flashSaleId,
            @RequestParam UUID sellerId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Applied to flash sale successfully",
                        applicationService.applyToFlashSale(flashSaleId, sellerId)));
    }

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<Page<SellerFlashSaleApplication>>> getMyApplications(
            @RequestParam UUID sellerId,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getSellerApplications(sellerId, pageable)));
    }

    @GetMapping("/application")
    public ResponseEntity<ApiResponse<SellerFlashSaleApplication>> getMyApplication(
            @RequestParam UUID flashSaleId,
            @RequestParam UUID sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                applicationService.getApplication(flashSaleId, sellerId)));
    }
}
