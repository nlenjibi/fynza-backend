package ecommerce.modules.order.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.order.dto.request.CancelOrderRequest;
import ecommerce.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Customer order operations")
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('order:write')")
    @Operation(summary = "Cancel an order")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) CancelOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        orderService.cancelOrder(id, principal.getId(),
                request != null ? request.getReason() : null);
        return ResponseEntity.ok(ApiResponse.success("Order cancelled successfully", null));
    }
}
