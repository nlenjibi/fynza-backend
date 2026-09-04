package ecommerce.modules.coupon.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.coupon.entity.SellerCoupon;
import ecommerce.modules.coupon.entity.SellerCoupon.DiscountType;
import ecommerce.modules.coupon.entity.SellerCoupon.Status;
import ecommerce.modules.coupon.service.SellerCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/coupons")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
public class SellerCouponController {

    private final SellerCouponService sellerCouponService;

    @PostMapping
    public ResponseEntity<ApiResponse<SellerCoupon>> createCoupon(
            @RequestParam UUID sellerId,
            @RequestParam String code,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam DiscountType discountType,
            @RequestParam BigDecimal discountValue,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) Integer maxUses,
            @RequestParam(required = false) Integer maxUsesPerUser,
            @RequestParam LocalDateTime validFrom,
            @RequestParam LocalDateTime validUntil) {

        SellerCoupon coupon = sellerCouponService.createCoupon(
                sellerId, code, name, description, discountType, discountValue,
                minOrderAmount, maxDiscountAmount, maxUses, maxUsesPerUser,
                validFrom, validUntil);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Coupon created successfully", coupon));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<SellerCoupon>>> getCoupons(
            @RequestParam UUID sellerId,
            @RequestParam(required = false) Status status,
            Pageable pageable) {
        Page<SellerCoupon> coupons = (status != null)
                ? sellerCouponService.getSellerCouponsByStatus(sellerId, status, pageable)
                : sellerCouponService.getSellerCoupons(sellerId, pageable);
        return ResponseEntity.ok(ApiResponse.success(coupons));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerCoupon>> getCoupon(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(sellerCouponService.getCoupon(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SellerCoupon>> updateCoupon(
            @PathVariable UUID id,
            @RequestParam UUID sellerId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) DiscountType discountType,
            @RequestParam(required = false) BigDecimal discountValue,
            @RequestParam(required = false) BigDecimal minOrderAmount,
            @RequestParam(required = false) BigDecimal maxDiscountAmount,
            @RequestParam(required = false) LocalDateTime validFrom,
            @RequestParam(required = false) LocalDateTime validUntil) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon updated successfully",
                sellerCouponService.updateCoupon(id, sellerId, name, description, discountType,
                        discountValue, minOrderAmount, maxDiscountAmount, validFrom, validUntil)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCoupon(
            @PathVariable UUID id,
            @RequestParam UUID sellerId) {
        sellerCouponService.deleteCoupon(id, sellerId);
        return ResponseEntity.ok(ApiResponse.success("Coupon deleted successfully", null));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<ApiResponse<SellerCoupon>> pauseCoupon(
            @PathVariable UUID id,
            @RequestParam UUID sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon paused successfully",
                sellerCouponService.pauseCoupon(id, sellerId)));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<SellerCoupon>> activateCoupon(
            @PathVariable UUID id,
            @RequestParam UUID sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Coupon activated successfully",
                sellerCouponService.activateCoupon(id, sellerId)));
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<ApiResponse<Boolean>> validateCoupon(
            @PathVariable String code,
            @RequestParam UUID sellerId) {
        return ResponseEntity.ok(ApiResponse.success(
                sellerCouponService.isValidCoupon(code, sellerId)));
    }
}
