package ecommerce.modules.inventory.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.inventory.dto.request.CreateTransferRequest;
import ecommerce.modules.inventory.dto.request.ReceiveTransferRequest;
import ecommerce.modules.inventory.dto.response.InventoryTransferResponse;
import ecommerce.modules.inventory.service.InventoryTransferService;
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
@RequestMapping("/v1/seller/inventory-transfers")
@RequiredArgsConstructor
@Tag(name = "Inventory — Transfers", description = "Stock transfer mutations")
public class InventoryTransferController {

    private final InventoryTransferService transferService;

    @PostMapping
    @PreAuthorize("hasAuthority('inventory.transfer.create')")
    @Operation(summary = "Request a stock transfer between locations")
    public ResponseEntity<ApiResponse<InventoryTransferResponse>> requestTransfer(
            @Valid @RequestBody CreateTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Transfer requested",
                        transferService.requestTransfer(request, principal.getId())));
    }

    @PostMapping("/{publicId}/approve")
    @PreAuthorize("hasAuthority('inventory.transfer.approve')")
    @Operation(summary = "Approve a transfer request")
    public ResponseEntity<ApiResponse<InventoryTransferResponse>> approveTransfer(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Transfer approved",
                transferService.approveTransfer(publicId, principal.getId())));
    }

    @PostMapping("/{publicId}/receive")
    @PreAuthorize("hasAuthority('inventory.transfer.receive')")
    @Operation(summary = "Receive a transfer and update stock")
    public ResponseEntity<ApiResponse<InventoryTransferResponse>> receiveTransfer(
            @PathVariable UUID publicId,
            @Valid @RequestBody ReceiveTransferRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Transfer received",
                transferService.receiveTransfer(publicId, request, principal.getId())));
    }

    @PostMapping("/{publicId}/cancel")
    @PreAuthorize("hasAuthority('inventory.transfer.cancel')")
    @Operation(summary = "Cancel a transfer")
    public ResponseEntity<ApiResponse<InventoryTransferResponse>> cancelTransfer(
            @PathVariable UUID publicId,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success("Transfer cancelled",
                transferService.cancelTransfer(publicId)));
    }
}
