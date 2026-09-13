package ecommerce.modules.inventory.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.inventory.dto.request.AdjustStockRequest;
import ecommerce.modules.inventory.dto.request.CreateInventoryRequest;
import ecommerce.modules.inventory.dto.request.ReserveStockRequest;
import ecommerce.modules.inventory.dto.request.UpdateInventoryRequest;
import ecommerce.modules.inventory.dto.response.InventoryReservationResponse;
import ecommerce.modules.inventory.dto.response.InventoryResponse;
import ecommerce.modules.inventory.dto.response.StockMovementResponse;
import ecommerce.modules.inventory.service.InventoryReservationService;
import ecommerce.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory", description = "Inventory management mutations")
public class InventoryController {

    private final InventoryService            inventoryService;
    private final InventoryReservationService reservationService;

    @PostMapping
    @PreAuthorize("hasAuthority('inventory.create')")
    @Operation(summary = "Create an inventory record for a product/variant at a location")
    public ResponseEntity<ApiResponse<InventoryResponse>> createInventory(
            @Valid @RequestBody CreateInventoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Inventory created",
                        inventoryService.createInventory(request, principal.getId())));
    }

    @PatchMapping("/{publicId}")
    @PreAuthorize("hasAuthority('inventory.update')")
    @Operation(summary = "Update inventory settings")
    public ResponseEntity<ApiResponse<InventoryResponse>> updateInventory(
            @PathVariable UUID publicId,
            @Valid @RequestBody UpdateInventoryRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Inventory updated",
                inventoryService.updateInventory(publicId, request, principal.getId())));
    }

    @PostMapping("/{publicId}/adjust")
    @PreAuthorize("hasAuthority('inventory.adjust')")
    @Operation(summary = "Adjust stock quantity")
    public ResponseEntity<ApiResponse<StockMovementResponse>> adjustStock(
            @PathVariable UUID publicId,
            @Valid @RequestBody AdjustStockRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Stock adjusted",
                inventoryService.adjustStock(publicId, request, principal.getId())));
    }

    @PostMapping("/{publicId}/reserve")
    @PreAuthorize("hasAuthority('inventory.reserve')")
    @Operation(summary = "Reserve stock for an order")
    public ResponseEntity<ApiResponse<InventoryReservationResponse>> reserve(
            @PathVariable UUID publicId,
            @Valid @RequestBody ReserveStockRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Stock reserved",
                        reservationService.reserve(publicId, request)));
    }

    @PostMapping("/reservations/{reservationPublicId}/release")
    @PreAuthorize("hasAuthority('inventory.release')")
    @Operation(summary = "Release a stock reservation")
    public ResponseEntity<ApiResponse<InventoryReservationResponse>> release(
            @PathVariable UUID reservationPublicId) {
        return ResponseEntity.ok(ApiResponse.success("Reservation released",
                reservationService.release(reservationPublicId)));
    }

    @PostMapping("/reservations/{reservationPublicId}/confirm")
    @PreAuthorize("hasAuthority('inventory.reserve')")
    @Operation(summary = "Confirm a stock reservation (on payment success)")
    public ResponseEntity<ApiResponse<InventoryReservationResponse>> confirm(
            @PathVariable UUID reservationPublicId) {
        return ResponseEntity.ok(ApiResponse.success("Reservation confirmed",
                reservationService.confirm(reservationPublicId)));
    }
}
