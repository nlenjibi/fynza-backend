package ecommerce.modules.store.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.store.dto.*;
import ecommerce.modules.store.service.StoreService;
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
@RequestMapping("/v1/seller")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SELLER')")
@Tag(name = "Store Management", description = "Seller store profile and settings")
public class StoreController {

    private final StoreService storeService;

    // ── Store profile ─────────────────────────────────────────────────────────

    @GetMapping("/store")
    @Operation(summary = "Get store info")
    public ResponseEntity<ApiResponse<StoreResponse>> getStore(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Store retrieved successfully",
                storeService.getStore(principal.getId())));
    }

    @PutMapping("/store")
    @Operation(summary = "Update store info")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @Valid @RequestBody UpdateStoreRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Store updated successfully",
                storeService.updateStore(principal.getId(), request)));
    }

    // ── Payment settings ──────────────────────────────────────────────────────

    @GetMapping("/settings/payment")
    @Operation(summary = "Get payment settings")
    public ResponseEntity<ApiResponse<SellerPaymentSettingsResponse>> getPaymentSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Payment settings retrieved",
                storeService.getPaymentSettings(principal.getId())));
    }

    @PutMapping("/settings/payment")
    @Operation(summary = "Update payment settings")
    public ResponseEntity<ApiResponse<SellerPaymentSettingsResponse>> updatePaymentSettings(
            @Valid @RequestBody SellerPaymentSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Payment settings updated",
                storeService.updatePaymentSettings(principal.getId(), request)));
    }

    // ── Shipping settings ─────────────────────────────────────────────────────

    @GetMapping("/settings/shipping")
    @Operation(summary = "Get shipping settings")
    public ResponseEntity<ApiResponse<SellerShippingSettingsResponse>> getShippingSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Shipping settings retrieved",
                storeService.getShippingSettings(principal.getId())));
    }

    @PutMapping("/settings/shipping")
    @Operation(summary = "Update shipping settings")
    public ResponseEntity<ApiResponse<SellerShippingSettingsResponse>> updateShippingSettings(
            @Valid @RequestBody SellerShippingSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Shipping settings updated",
                storeService.updateShippingSettings(principal.getId(), request)));
    }

    // ── Shipping zones ────────────────────────────────────────────────────────

    @GetMapping("/settings/shipping/zones")
    @Operation(summary = "Get shipping zones")
    public ResponseEntity<ApiResponse<List<ShippingZoneResponse>>> getShippingZones(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Shipping zones retrieved",
                storeService.getShippingZones(principal.getId())));
    }

    @PostMapping("/settings/shipping/zones")
    @Operation(summary = "Create shipping zone")
    public ResponseEntity<ApiResponse<ShippingZoneResponse>> createShippingZone(
            @Valid @RequestBody ShippingZoneRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping zone created",
                        storeService.createShippingZone(principal.getId(), request)));
    }

    @PutMapping("/settings/shipping/zones/{zoneId}")
    @Operation(summary = "Update shipping zone")
    public ResponseEntity<ApiResponse<ShippingZoneResponse>> updateShippingZone(
            @PathVariable UUID zoneId,
            @Valid @RequestBody ShippingZoneRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Shipping zone updated",
                storeService.updateShippingZone(principal.getId(), zoneId, request)));
    }

    @DeleteMapping("/settings/shipping/zones/{zoneId}")
    @Operation(summary = "Delete shipping zone")
    public ResponseEntity<ApiResponse<Void>> deleteShippingZone(
            @PathVariable UUID zoneId,
            @AuthenticationPrincipal UserPrincipal principal) {
        storeService.deleteShippingZone(principal.getId(), zoneId);
        return ResponseEntity.ok(ApiResponse.success("Shipping zone deleted", null));
    }

    // ── Notification settings ─────────────────────────────────────────────────

    @GetMapping("/settings/notifications")
    @Operation(summary = "Get notification settings")
    public ResponseEntity<ApiResponse<SellerNotificationSettingsResponse>> getNotificationSettings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Notification settings retrieved",
                storeService.getNotificationSettings(principal.getId())));
    }

    @PutMapping("/settings/notifications")
    @Operation(summary = "Update notification settings")
    public ResponseEntity<ApiResponse<SellerNotificationSettingsResponse>> updateNotificationSettings(
            @RequestBody SellerNotificationSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Notification settings updated",
                storeService.updateNotificationSettings(principal.getId(), request)));
    }
}
