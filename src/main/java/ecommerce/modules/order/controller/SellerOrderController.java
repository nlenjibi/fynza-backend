package ecommerce.modules.order.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.order.dto.request.UpdateOrderStatusRequest;
import ecommerce.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/seller/orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('order:write')")
@Tag(name = "Seller Orders", description = "Seller order status management")
public class SellerOrderController {

    private final OrderService orderService;

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update seller order status")
    public ResponseEntity<ApiResponse<Void>> updateStatus(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        orderService.sellerUpdateStatus(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Order status updated", null));
    }
}
