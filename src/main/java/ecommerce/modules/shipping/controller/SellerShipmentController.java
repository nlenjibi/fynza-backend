package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.shipping.dto.request.*;
import ecommerce.modules.shipping.dto.response.*;
import ecommerce.modules.shipping.enums.FulfillmentStatus;
import ecommerce.modules.shipping.service.FulfillmentService;
import ecommerce.modules.shipping.service.ShipmentService;
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
@RequestMapping("/v1/seller/shipping")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('shipping:write')")
@Tag(name = "Seller Shipping", description = "Seller fulfillment and shipment management")
public class SellerShipmentController {

    private final FulfillmentService fulfillmentService;
    private final ShipmentService shipmentService;

    // =========================================================================
    // Fulfillments
    // =========================================================================

    @PostMapping("/fulfillments")
    @Operation(summary = "Create a fulfillment for a seller order")
    public ResponseEntity<ApiResponse<FulfillmentResponse>> createFulfillment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateFulfillmentRequest request) {
        FulfillmentResponse response = fulfillmentService.createFulfillment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fulfillment created", response));
    }

    @PatchMapping("/fulfillments/{fulfillmentId}/status")
    @Operation(summary = "Update fulfillment status (e.g. PROCESSING → PACKED)")
    public ResponseEntity<ApiResponse<FulfillmentResponse>> updateFulfillmentStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID fulfillmentId,
            @RequestParam FulfillmentStatus status,
            @RequestParam(required = false) String notes) {
        FulfillmentResponse response = fulfillmentService.updateStatus(
                principal.getId(), fulfillmentId, status, notes);
        return ResponseEntity.ok(ApiResponse.success("Fulfillment status updated", response));
    }

    // =========================================================================
    // Shipments
    // =========================================================================

    @PostMapping("/shipments")
    @Operation(summary = "Create a new shipment for a fulfillment")
    public ResponseEntity<ApiResponse<ShipmentResponse>> createShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateShipmentRequest request) {
        ShipmentResponse response = shipmentService.createShipment(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Shipment created", response));
    }

    @PostMapping("/shipments/{shipmentId}/label")
    @Operation(summary = "Generate a shipping label via the active carrier provider")
    public ResponseEntity<ApiResponse<ShipmentResponse>> generateLabel(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID shipmentId) {
        ShipmentResponse response = shipmentService.generateLabel(principal.getId(), shipmentId);
        return ResponseEntity.ok(ApiResponse.success("Label generated", response));
    }

    @PatchMapping("/shipments/{shipmentId}/status")
    @Operation(summary = "Update shipment status")
    public ResponseEntity<ApiResponse<ShipmentResponse>> updateShipmentStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID shipmentId,
            @Valid @RequestBody UpdateShipmentStatusRequest request) {
        ShipmentResponse response = shipmentService.updateStatus(principal.getId(), shipmentId, request);
        return ResponseEntity.ok(ApiResponse.success("Shipment status updated", response));
    }

    @PostMapping("/shipments/{shipmentId}/tracking-events")
    @Operation(summary = "Record a manual tracking event")
    public ResponseEntity<ApiResponse<TrackingEventResponse>> recordTrackingEvent(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody RecordTrackingEventRequest request) {
        TrackingEventResponse response = shipmentService.recordTrackingEvent(shipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tracking event recorded", response));
    }

    @DeleteMapping("/shipments/{shipmentId}")
    @Operation(summary = "Cancel a shipment")
    public ResponseEntity<ApiResponse<Void>> cancelShipment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID shipmentId) {
        shipmentService.cancelShipment(principal.getId(), shipmentId);
        return ResponseEntity.ok(ApiResponse.success("Shipment cancelled", null));
    }
}
