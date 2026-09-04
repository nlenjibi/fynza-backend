package ecommerce.modules.refund.controller;

import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.refund.dto.RefundRequest;
import ecommerce.modules.refund.dto.RefundResponse;
import ecommerce.modules.refund.dto.RefundStatsResponse;
import ecommerce.modules.refund.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/customer/refunds")
@RequiredArgsConstructor
@Tag(name = "Customer Refunds", description = "Customer refund request endpoints")
public class CustomerRefundController {

    private final RefundService refundService;

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Request refund", description = "Request a refund for an order")
    public ResponseEntity<ApiResponse<RefundResponse>> requestRefund(
            @Valid @RequestBody RefundRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = UUID.fromString(userDetails.getUsername());
        RefundResponse refund = refundService.createRefund(request, customerId);
        return ResponseEntity.ok(ApiResponse.success("Refund request submitted successfully", refund));
    }

    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my refunds", description = "Get all refund requests for the authenticated customer")
    public ResponseEntity<ApiResponse<Page<RefundResponse>>> getMyRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = UUID.fromString(userDetails.getUsername());
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<RefundResponse> refunds = refundService.getRefundsByCustomer(customerId, pageable);
        return ResponseEntity.ok(ApiResponse.success("Refunds retrieved successfully", refunds));
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get refund details", description = "Get details of a specific refund")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundDetails(
            @PathVariable UUID refundId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = UUID.fromString(userDetails.getUsername());
        RefundResponse refund = refundService.getRefundById(refundId);
        
        if (!refund.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Refund details retrieved successfully", refund));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get refund by order", description = "Get refund details for a specific order")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundByOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = UUID.fromString(userDetails.getUsername());
        RefundResponse refund = refundService.getRefundByOrderId(orderId);
        
        if (!refund.getCustomerId().equals(customerId)) {
            return ResponseEntity.status(403)
                    .body(ApiResponse.error("Access denied"));
        }
        
        return ResponseEntity.ok(ApiResponse.success("Refund details retrieved successfully", refund));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Operation(summary = "Get my refund statistics", description = "Get refund statistics for the authenticated customer")
    public ResponseEntity<ApiResponse<RefundStatsResponse>> getMyRefundStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID customerId = UUID.fromString(userDetails.getUsername());
        Page<RefundResponse> refunds = refundService.getRefundsByCustomer(customerId, Pageable.unpaged());
        
        RefundStatsResponse stats = RefundStatsResponse.builder()
                .totalRefunds(refunds.getTotalElements())
                .build();
        
        return ResponseEntity.ok(ApiResponse.success("Refund statistics retrieved successfully", stats));
    }
}
