package ecommerce.modules.order.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.security.UserPrincipal;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.request.CreateOrderRequest;
import ecommerce.modules.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/checkout")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Checkout", description = "Checkout endpoint — creates an order from the customer's active cart")
public class CheckoutController {

    private final OrderService orderService;

    @PostMapping
    @PreAuthorize("hasAuthority('order:write')")
    @Operation(
            summary = "Create order from cart",
            description = "Creates an order from the customer's active cart. Clears the cart on success.")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {

        log.info("POST /v1/checkout — user={}", principal.getId());
        OrderResponse order = orderService.createOrder(principal.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }
}
