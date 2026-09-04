package ecommerce.modules.refund.service;

import ecommerce.common.enums.RefundStatus;
import ecommerce.modules.refund.dto.RefundRequest;
import ecommerce.modules.refund.dto.RefundResponse;
import ecommerce.modules.refund.dto.RefundStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface RefundService {

    RefundResponse createRefund(RefundRequest request, UUID customerId);

    RefundResponse getRefundById(UUID refundId);

    RefundResponse getRefundByOrderId(UUID orderId);

    RefundResponse getRefundByNumber(String refundNumber);

    Page<RefundResponse> getAllRefunds(Pageable pageable);

    Page<RefundResponse> getRefundsByStatus(RefundStatus status, Pageable pageable);

    Page<RefundResponse> getRefundsByCustomer(UUID customerId, Pageable pageable);

    Page<RefundResponse> getRefundsBySeller(UUID sellerId, Pageable pageable);

    Page<RefundResponse> searchRefunds(
            RefundStatus status,
            UUID customerId,
            UUID sellerId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query,
            Pageable pageable);

    RefundResponse approveRefund(UUID refundId, UUID adminId, String adminNote);

    RefundResponse rejectRefund(UUID refundId, UUID adminId, String rejectionReason);

    RefundResponse completeRefund(UUID refundId, String transactionId);

    RefundStatsResponse getRefundStats();

    RefundStatsResponse getRefundStatsBySeller(UUID sellerId);

    String exportRefundsToCSV(
            RefundStatus status,
            UUID customerId,
            UUID sellerId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query);
}
