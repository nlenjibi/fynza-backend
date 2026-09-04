package ecommerce.modules.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecommerce.common.response.ApiResponse;
import ecommerce.modules.order.dto.CartOrderRequest;
import ecommerce.modules.order.dto.CreateOrderRequest;
import ecommerce.modules.order.dto.OrderResponse;
import ecommerce.modules.order.dto.PaymentProcessRequest;
import ecommerce.modules.order.service.OrderService;
import ecommerce.common.security.UserPrincipal;
import ecommerce.common.util.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * =================================================================
 * CHECKOUT CONTROLLER
 * =================================================================
 * 
 * PURPOSE:
 * Handles HTTP requests for checkout and payment processing operations.
 * Uses the consolidated OrderService for all operations.
 * 
 * LAYER: Controller (HTTP handling only, no business logic)
 * 
 * SECURITY:
 * - All endpoints require CUSTOMER role
 * 
 * @author Fynza Backend Team
 * @version 2.1
 */
@RestController
@RequestMapping("api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Checkout", description = "Checkout and payment processing endpoints")
public class CheckoutController {

    private final OrderService orderService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    /**
     * Creates an order from the customer's cart.
     * 
     * This endpoint is used for the checkout flow where:
     * 1. User has items in cart
     * 2. User selects shipping address and payment method
     * 3. Order is created and cart is cleared
     * 
     * IDEMPOTENCY:
     * - Accepts "Idempotency-Key" header with unique key
     * - Validates payload consistency (same key + different payload = error)
     * - Returns cached response for duplicate requests
     * 
     * @param cartId    The cart UUID to checkout
     * @param request   Optional checkout metadata (shipping address, payment method, etc.)
     * @param principal The authenticated user principal
     * @return The created order
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(
        summary = "Create order from cart", 
        description = "Create an order from the customer's cart. Use 'Idempotency-Key' header to prevent duplicate orders."
    )
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestParam UUID cartId,
            @RequestBody(required = false) CartOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("POST /api/v1/checkout - cartId={}, user={}, idempotencyKey={}", cartId, principal.getId(), idempotencyKey);
        
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.validatePayload(idempotencyKey, request)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.<OrderResponse>builder()
                                .message("Idempotency key already used with different payload")
                                .build());
            }
            
            Optional<String> cached = idempotencyService.check(idempotencyKey, request);
            if (cached.isPresent()) {
                try {
                    OrderResponse cachedOrder = objectMapper.readValue(cached.get(), OrderResponse.class);
                    return ResponseEntity.ok(ApiResponse.success("Order retrieved from cache (duplicate request)", cachedOrder));
                } catch (Exception e) {
                    log.error("Failed to deserialize cached order", e);
                }
            }
        }
        
        CreateOrderRequest createRequest = CreateOrderRequest.builder()
                .shippingAddressId(request != null && request.getShippingAddress() != null 
                        ? extractAddressId(request.getShippingAddress()) : null)
                .paymentMethod(request != null && request.getPaymentMethod() != null 
                        ? request.getPaymentMethod().name() : null)
                .idempotencyKey(idempotencyKey)
                .build();
        
        OrderResponse order = orderService.createOrder(createRequest, principal.getId());
        
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.save(idempotencyKey, request, order);
        }
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Order created successfully", order));
    }

    /**
     * Processes payment for an order.
     * 
     * This endpoint confirms the order and updates payment status to PAID.
     * In a real implementation, this would integrate with a payment gateway.
     * 
     * @param request   The payment processing request
     * @param principal The authenticated user principal
     * @return The updated order with confirmed status
     */
    @PostMapping("/payments/process")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Process payment", description = "Process payment for an order")
    public ResponseEntity<ApiResponse<OrderResponse>> processPayment(
            @RequestBody PaymentProcessRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        
        log.info("POST /api/v1/payments/process - orderId={}, user={}", request.getOrderId(), principal.getId());
        
        // Update order status to CONFIRMED (which also sets payment status to PAID)
        OrderResponse order = orderService.updateOrderStatus(request.getOrderId(), 
                ecommerce.common.enums.OrderStatus.CONFIRMED);
        
        return ResponseEntity.ok(ApiResponse.success("Payment processed successfully", order));
    }

    /**
     * Extracts address ID from shipping address string.
     * 
     * In a real implementation, this would parse or look up the address.
     * For now, returns null as address selection is handled separately.
     * 
     * @param shippingAddress The shipping address string
     * @return The address UUID, or null if not parseable
     */
    private UUID extractAddressId(String shippingAddress) {
        // This is a placeholder - in real implementation, this would
        // either parse the address string or lookup from user's saved addresses
        return null;
    }
}
