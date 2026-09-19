package ecommerce.modules.order.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.request.UpdateOrderStatusRequest;
import ecommerce.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('order:admin')")
@Tag(name = "Admin Orders", description = "Admin order management")
public class AdminOrderController {

    private final OrderService orderService;

    @PatchMapping("/{id}")
    @Operation(summary = "Update order status (admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderResponse order = orderService.adminUpdateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success("Order updated successfully", order));
    }
}
