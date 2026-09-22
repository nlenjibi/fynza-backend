package ecommerce.modules.pricing.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.pricing.dto.request.PriceOverrideRequest;
import ecommerce.modules.pricing.dto.request.UpdatePriceRequest;
import ecommerce.modules.pricing.dto.response.PriceResponse;
import ecommerce.modules.pricing.service.AdminPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/prices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin — Prices", description = "Admin price management mutations")
public class AdminPriceController {

    private final AdminPriceService adminPriceService;

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('price.update')")
    @Operation(summary = "Admin update of any price record")
    public ResponseEntity<ApiResponse<PriceResponse>> updatePrice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePriceRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price updated successfully",
                adminPriceService.updatePrice(id, request, adminId)));
    }

    @PostMapping("/{id}/override")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('price.override')")
    @Operation(summary = "Apply an audited administrative price override")
    public ResponseEntity<ApiResponse<PriceResponse>> overridePrice(
            @PathVariable UUID id,
            @Valid @RequestBody PriceOverrideRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID adminId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.ok(ApiResponse.success("Price override applied successfully",
                adminPriceService.overridePrice(id, request, adminId)));
    }
}
