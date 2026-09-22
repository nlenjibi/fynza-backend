package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.dto.request.*;
import ecommerce.modules.shipping.dto.response.*;
import ecommerce.modules.shipping.service.CarrierAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('shipping:admin')")
@Tag(name = "Admin Shipping", description = "Admin management of carriers, methods, zones, and rates")
public class AdminCarrierController {

    private final CarrierAdminService carrierAdminService;

    // =========================================================================
    // Carriers
    // =========================================================================

    @PostMapping("/carriers")
    @Operation(summary = "Create a shipping carrier")
    public ResponseEntity<ApiResponse<CarrierResponse>> createCarrier(
            @Valid @RequestBody CreateCarrierRequest request) {
        CarrierResponse response = carrierAdminService.createCarrier(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Carrier created", response));
    }

    @PatchMapping("/carriers/{carrierId}/toggle")
    @Operation(summary = "Enable or disable a carrier")
    public ResponseEntity<ApiResponse<CarrierResponse>> toggleCarrier(
            @PathVariable UUID carrierId,
            @RequestParam boolean active) {
        CarrierResponse response = carrierAdminService.toggleCarrier(carrierId, active);
        return ResponseEntity.ok(ApiResponse.success("Carrier updated", response));
    }

    // =========================================================================
    // Shipping Methods
    // =========================================================================

    @PostMapping("/methods")
    @Operation(summary = "Create a shipping method")
    public ResponseEntity<ApiResponse<ShippingMethodResponse>> createMethod(
            @Valid @RequestBody CreateShippingMethodRequest request) {
        ShippingMethodResponse response = carrierAdminService.createShippingMethod(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping method created", response));
    }

    @PatchMapping("/methods/{methodId}/toggle")
    @Operation(summary = "Enable or disable a shipping method")
    public ResponseEntity<ApiResponse<ShippingMethodResponse>> toggleMethod(
            @PathVariable UUID methodId,
            @RequestParam boolean active) {
        ShippingMethodResponse response = carrierAdminService.toggleMethod(methodId, active);
        return ResponseEntity.ok(ApiResponse.success("Shipping method updated", response));
    }

    // =========================================================================
    // Zones
    // =========================================================================

    @PostMapping("/zones")
    @Operation(summary = "Create a shipping zone")
    public ResponseEntity<ApiResponse<ShippingZoneResponse>> createZone(
            @Valid @RequestBody CreateShippingZoneRequest request) {
        ShippingZoneResponse response = carrierAdminService.createZone(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping zone created", response));
    }

    @PatchMapping("/zones/{zoneId}/toggle")
    @Operation(summary = "Enable or disable a shipping zone")
    public ResponseEntity<ApiResponse<ShippingZoneResponse>> toggleZone(
            @PathVariable UUID zoneId,
            @RequestParam boolean active) {
        ShippingZoneResponse response = carrierAdminService.toggleZone(zoneId, active);
        return ResponseEntity.ok(ApiResponse.success("Shipping zone updated", response));
    }

    // =========================================================================
    // Rates
    // =========================================================================

    @PostMapping("/rates")
    @Operation(summary = "Create a shipping rate for a method + zone combination")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> createRate(
            @Valid @RequestBody CreateShippingRateRequest request) {
        ShippingRateResponse response = carrierAdminService.createRate(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipping rate created", response));
    }

    @PatchMapping("/rates/{rateId}/toggle")
    @Operation(summary = "Enable or disable a shipping rate")
    public ResponseEntity<ApiResponse<ShippingRateResponse>> toggleRate(
            @PathVariable UUID rateId,
            @RequestParam boolean active) {
        ShippingRateResponse response = carrierAdminService.toggleRate(rateId, active);
        return ResponseEntity.ok(ApiResponse.success("Shipping rate updated", response));
    }
}
