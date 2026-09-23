package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.dto.request.DeliveryAttemptRequest;
import ecommerce.modules.shipping.dto.response.DeliveryAttemptResponse;
import ecommerce.modules.shipping.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/shipping/shipments/{shipmentId}/delivery-attempts")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Delivery Attempts", description = "Record and query delivery attempt history for shipments")
public class DeliveryAttemptController {

    private final DeliveryService deliveryService;

    @PostMapping
    @Operation(summary = "Record a delivery attempt for a shipment")
    public ResponseEntity<ApiResponse<DeliveryAttemptResponse>> recordAttempt(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody DeliveryAttemptRequest request) {
        DeliveryAttemptResponse response = deliveryService.recordAttempt(shipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Delivery attempt recorded", response));
    }
}
