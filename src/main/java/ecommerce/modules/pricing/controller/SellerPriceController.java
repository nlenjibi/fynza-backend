package ecommerce.modules.pricing.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.pricing.dto.request.CreatePriceRequest;
import ecommerce.modules.pricing.dto.request.CreatePriceTierRequest;
import ecommerce.modules.pricing.dto.request.SchedulePriceRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.dto.response.PriceTierResponse;
import ecommerce.modules.pricing.service.PriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/prices")
@RequiredArgsConstructor
@Tag(name = "Seller — Prices", description = "Seller price management mutations")
public class SellerPriceController {

    private final PriceService priceService;

    @PostMapping
    @PreAuthorize("hasAuthority('price.create')")
    @Operation(summary = "Create a new price for a product or variant")
    public ResponseEntity<ApiResponse<PriceResponse>> createPrice(
            @Valid @RequestBody CreatePriceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Price created successfully",
                        priceService.createPrice(request, actorId)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('price.update')")
    @Operation(summary = "Update a draft or inactive price")
    public ResponseEntity<ApiResponse<PriceResponse>> updatePrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price updated successfully",
                priceService.updatePrice(id, request, actorId)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('price.activate')")
    @Operation(summary = "Activate a price, making it the effective price for the product")
    public ResponseEntity<ApiResponse<PriceResponse>> activatePrice(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price activated successfully",
                priceService.activatePrice(id, actorId)));
    }

    @PostMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('price.disable')")
    @Operation(summary = "Disable an active price")
    public ResponseEntity<ApiResponse<PriceResponse>> disablePrice(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price disabled successfully",
                priceService.disablePrice(id, actorId)));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAuthority('price.schedule')")
    @Operation(summary = "Schedule a price to become effective at a future time")
    public ResponseEntity<ApiResponse<PriceResponse>> schedulePrice(
            @PathVariable UUID id,
            @Valid @RequestBody SchedulePriceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price scheduled successfully",
                priceService.schedulePrice(id, request, actorId)));
    }

    // ── Price Tiers ───────────────────────────────────────────────────────────

    @PostMapping("/tiers")
    @PreAuthorize("hasAuthority('price.create')")
    @Operation(summary = "Add a quantity-based pricing tier to a price")
    public ResponseEntity<ApiResponse<PriceTierResponse>> createTier(
            @Valid @RequestBody CreatePriceTierRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success("Price tier created successfully",
                        priceService.createTier(request, actorId)));
    }

    @PatchMapping("/tiers/{tierId}")
    @PreAuthorize("hasAuthority('price.update')")
    @Operation(summary = "Update a price tier")
    public ResponseEntity<ApiResponse<PriceTierResponse>> updateTier(
            @PathVariable UUID tierId,
            @Valid @RequestBody CreatePriceTierRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price tier updated successfully",
                priceService.updateTier(tierId, request, actorId)));
    }

    @DeleteMapping("/tiers/{tierId}")
    @PreAuthorize("hasAuthority('price.delete')")
    @Operation(summary = "Remove a price tier")
    public ResponseEntity<ApiResponse<Void>> deleteTier(
            @PathVariable UUID tierId,
            @AuthenticationPrincipal Jwt jwt) {
        UUID actorId = UUID.fromString(jwt.getSubject());
        priceService.deleteTier(tierId, actorId);
        return ResponseEntity.ok(ApiResponse.success("Price tier deleted successfully", null));
    }
}
