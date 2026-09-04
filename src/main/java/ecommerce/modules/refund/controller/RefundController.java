package ecommerce.modules.refund.controller;

import ecommerce.common.enums.RefundStatus;
import ecommerce.common.response.ApiResponse;
import ecommerce.common.response.PaginatedResponse;
import ecommerce.modules.refund.dto.RefundResponse;
import ecommerce.modules.refund.dto.RefundStatsResponse;
import ecommerce.modules.refund.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/refunds")
@RequiredArgsConstructor
@Tag(name = "Admin Refunds", description = "Admin refund management endpoints")
public class RefundController {

    private final RefundService refundService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all refunds", description = "Get all refunds with optional filters")
    public ResponseEntity<ApiResponse<PaginatedResponse<RefundResponse>>> getAllRefunds(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) LocalDateTime dateFrom,
            @RequestParam(required = false) LocalDateTime dateTo,
            @RequestParam(required = false) String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        Page<RefundResponse> refunds = refundService.searchRefunds(
                status, customerId, sellerId, dateFrom, dateTo, query, pageable);
        return ResponseEntity.ok(ApiResponse.success("Refunds retrieved successfully",
                PaginatedResponse.from(refunds)));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get refund statistics", description = "Get refund statistics for admin dashboard")
    public ResponseEntity<ApiResponse<RefundStatsResponse>> getRefundStats() {
        RefundStatsResponse stats = refundService.getRefundStats();
        return ResponseEntity.ok(ApiResponse.success("Refund statistics retrieved successfully", stats));
    }

    @GetMapping("/{refundId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get refund by ID", description = "Get a specific refund by ID")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundById(@PathVariable UUID refundId) {
        RefundResponse refund = refundService.getRefundById(refundId);
        return ResponseEntity.ok(ApiResponse.success("Refund retrieved successfully", refund));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get refund by order ID", description = "Get refund for a specific order")
    public ResponseEntity<ApiResponse<RefundResponse>> getRefundByOrderId(@PathVariable UUID orderId) {
        RefundResponse refund = refundService.getRefundByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.success("Refund retrieved successfully", refund));
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Export refunds to CSV", description = "Export refunds as CSV file")
    public ResponseEntity<byte[]> exportRefunds(
            @RequestParam(required = false) RefundStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID sellerId,
            @RequestParam(required = false) LocalDateTime dateFrom,
            @RequestParam(required = false) LocalDateTime dateTo,
            @RequestParam(required = false) String query) {
        String csv = refundService.exportRefundsToCSV(status, customerId, sellerId, dateFrom, dateTo, query);
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=refunds.csv")
                .body(csv.getBytes());
    }

    @PatchMapping("/{refundId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Approve refund", description = "Approve a pending refund")
    public ResponseEntity<ApiResponse<RefundResponse>> approveRefund(
            @PathVariable UUID refundId,
            @RequestParam(required = false) String adminNote,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        RefundResponse refund = refundService.approveRefund(refundId, adminId, adminNote);
        return ResponseEntity.ok(ApiResponse.success("Refund approved successfully", refund));
    }

    @PatchMapping("/{refundId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Reject refund", description = "Reject a pending refund")
    public ResponseEntity<ApiResponse<RefundResponse>> rejectRefund(
            @PathVariable UUID refundId,
            @RequestParam String rejectionReason,
            @AuthenticationPrincipal UserDetails userDetails) {
        UUID adminId = UUID.fromString(userDetails.getUsername());
        RefundResponse refund = refundService.rejectRefund(refundId, adminId, rejectionReason);
        return ResponseEntity.ok(ApiResponse.success("Refund rejected successfully", refund));
    }

    @PatchMapping("/{refundId}/complete")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete refund", description = "Mark a refund as completed with transaction ID")
    public ResponseEntity<ApiResponse<RefundResponse>> completeRefund(
            @PathVariable UUID refundId,
            @RequestParam(required = false) String transactionId) {
        RefundResponse refund = refundService.completeRefund(refundId, transactionId);
        return ResponseEntity.ok(ApiResponse.success("Refund completed successfully", refund));
    }
}
