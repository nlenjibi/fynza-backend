package ecommerce.modules.shipping.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.shipping.dto.request.ResolveExceptionRequest;
import ecommerce.modules.shipping.dto.request.ShipmentExceptionRequest;
import ecommerce.modules.shipping.dto.response.ShipmentExceptionResponse;
import ecommerce.modules.shipping.service.ShipmentExceptionService;
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
@RequiredArgsConstructor
@Tag(name = "Shipment Exceptions", description = "Create and resolve shipment exceptions")
public class ShipmentExceptionController {

    private final ShipmentExceptionService exceptionService;

    @PostMapping("/v1/seller/shipping/shipments/{shipmentId}/exceptions")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Create a shipment exception")
    public ResponseEntity<ApiResponse<ShipmentExceptionResponse>> createException(
            @PathVariable UUID shipmentId,
            @Valid @RequestBody ShipmentExceptionRequest request) {
        ShipmentExceptionResponse response = exceptionService.createException(shipmentId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exception created", response));
    }

    @PatchMapping("/v1/admin/shipping/exceptions/{exceptionId}/resolve")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @Operation(summary = "Resolve a shipment exception")
    public ResponseEntity<ApiResponse<ShipmentExceptionResponse>> resolveException(
            @PathVariable UUID exceptionId,
            @RequestBody ResolveExceptionRequest request) {
        ShipmentExceptionResponse response = exceptionService.resolveException(exceptionId, request);
        return ResponseEntity.ok(ApiResponse.success("Exception resolved", response));
    }
}
