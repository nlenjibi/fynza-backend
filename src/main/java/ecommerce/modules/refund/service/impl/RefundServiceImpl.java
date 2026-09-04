package ecommerce.modules.refund.service.impl;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.enums.RefundStatus;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.refund.dto.RefundRequest;
import ecommerce.modules.refund.dto.RefundResponse;
import ecommerce.modules.refund.dto.RefundStatsResponse;
import ecommerce.modules.refund.entity.Refund;
import ecommerce.modules.refund.repository.RefundRepository;
import ecommerce.modules.refund.service.RefundService;
import ecommerce.modules.user.entity.User;
import ecommerce.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    private static final List<RefundStatus> COMPLETED_STATUSES = Arrays.asList(
            RefundStatus.APPROVED, RefundStatus.COMPLETED);
    private static final List<RefundStatus> PENDING_STATUSES = List.of(RefundStatus.PENDING);

    @Override
    @Transactional
    public RefundResponse createRefund(RefundRequest request, UUID customerId) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Order does not belong to this customer");
        }

        if (order.getStatus() != OrderStatus.PROCESSING && order.getStatus() != OrderStatus.IN_TRANSIT) {
            throw new IllegalArgumentException("Refund can only be requested for PROCESSING or IN_TRANSIT orders");
        }

        if (refundRepository.findByOrderId(order.getId()).isPresent()) {
            throw new IllegalArgumentException("Refund already requested for this order");
        }

        if (request.getAmount().compareTo(order.getTotalAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed order total");
        }

        UUID sellerId = order.getOrderItems().isEmpty() ? null : order.getOrderItems().get(0).getProduct().getSeller().getId();

        Refund refund = Refund.builder()
                .refundNumber(Refund.generateRefundNumber())
                .order(order)
                .customerId(customerId)
                .sellerId(sellerId)
                .amount(request.getAmount())
                .status(RefundStatus.PENDING)
                .reason(request.getReason())
                .customerNote(request.getCustomerNote())
                .build();

        order.setStatus(OrderStatus.REFUND_REQUESTED);
        orderRepository.save(order);

        Refund saved = refundRepository.save(refund);
        log.info("Refund created: {} for order: {}", saved.getRefundNumber(), order.getOrderNumber());

        return mapToResponse(saved);
    }

    @Override
    public RefundResponse getRefundById(UUID refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        return mapToResponse(refund);
    }

    @Override
    public RefundResponse getRefundByOrderId(UUID orderId) {
        Refund refund = refundRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found for this order"));
        return mapToResponse(refund);
    }

    @Override
    public RefundResponse getRefundByNumber(String refundNumber) {
        Refund refund = refundRepository.findByRefundNumber(refundNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        return mapToResponse(refund);
    }

    @Override
    public Page<RefundResponse> getAllRefunds(Pageable pageable) {
        return refundRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    public Page<RefundResponse> getRefundsByStatus(RefundStatus status, Pageable pageable) {
        return refundRepository.findByStatus(status, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<RefundResponse> getRefundsByCustomer(UUID customerId, Pageable pageable) {
        return refundRepository.findByCustomerId(customerId, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<RefundResponse> getRefundsBySeller(UUID sellerId, Pageable pageable) {
        return refundRepository.findBySellerId(sellerId, pageable).map(this::mapToResponse);
    }

    @Override
    public Page<RefundResponse> searchRefunds(
            RefundStatus status,
            UUID customerId,
            UUID sellerId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query,
            Pageable pageable) {
        return refundRepository.searchRefunds(status, customerId, sellerId, dateFrom, dateTo, query, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public RefundResponse approveRefund(UUID refundId, UUID adminId, String adminNote) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalArgumentException("Only pending refunds can be approved");
        }

        refund.setStatus(RefundStatus.APPROVED);
        refund.setAdminNote(adminNote);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setReviewedBy(adminId);

        Order order = refund.getOrder();
        order.setStatus(OrderStatus.REFUNDED);
        orderRepository.save(order);

        Refund saved = refundRepository.save(refund);
        log.info("Refund approved: {} by admin: {}", saved.getRefundNumber(), adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RefundResponse rejectRefund(UUID refundId, UUID adminId, String rejectionReason) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new IllegalArgumentException("Only pending refunds can be rejected");
        }

        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectionReason(rejectionReason);
        refund.setAdminNote(rejectionReason);
        refund.setReviewedAt(LocalDateTime.now());
        refund.setReviewedBy(adminId);

        Order order = refund.getOrder();
        if (order.getStatus() == OrderStatus.REFUND_REQUESTED) {
            order.setStatus(OrderStatus.DELIVERED);
            orderRepository.save(order);
        }

        Refund saved = refundRepository.save(refund);
        log.info("Refund rejected: {} by admin: {}", saved.getRefundNumber(), adminId);

        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public RefundResponse completeRefund(UUID refundId, String transactionId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));

        if (refund.getStatus() != RefundStatus.APPROVED) {
            throw new IllegalArgumentException("Only approved refunds can be completed");
        }

        refund.setStatus(RefundStatus.COMPLETED);
        refund.setTransactionId(transactionId);
        refund.setCompletedAt(LocalDateTime.now());

        Refund saved = refundRepository.save(refund);
        log.info("Refund completed: {} with transaction: {}", saved.getRefundNumber(), transactionId);

        return mapToResponse(saved);
    }

    @Override
    public RefundStatsResponse getRefundStats() {
        long total = refundRepository.countAll();
        long pending = refundRepository.countByStatus(RefundStatus.PENDING);
        long approved = refundRepository.countByStatus(RefundStatus.APPROVED);
        long rejected = refundRepository.countByStatus(RefundStatus.REJECTED);
        long completed = refundRepository.countByStatus(RefundStatus.COMPLETED);

        BigDecimal totalAmount = refundRepository.sumAmountByStatus(
                Arrays.asList(RefundStatus.APPROVED, RefundStatus.COMPLETED));
        BigDecimal pendingAmount = refundRepository.sumAmountByStatus(RefundStatus.PENDING);
        BigDecimal approvedAmount = refundRepository.sumAmountByStatus(RefundStatus.APPROVED);
        BigDecimal completedAmount = refundRepository.sumAmountByStatus(RefundStatus.COMPLETED);

        Map<String, Long> byStatus = new HashMap<>();
        byStatus.put("PENDING", pending);
        byStatus.put("APPROVED", approved);
        byStatus.put("REJECTED", rejected);
        byStatus.put("COMPLETED", completed);

        Map<String, BigDecimal> amountByStatus = new HashMap<>();
        amountByStatus.put("PENDING", pendingAmount != null ? pendingAmount : BigDecimal.ZERO);
        amountByStatus.put("APPROVED", approvedAmount != null ? approvedAmount : BigDecimal.ZERO);
        amountByStatus.put("COMPLETED", completedAmount != null ? completedAmount : BigDecimal.ZERO);

        return RefundStatsResponse.builder()
                .totalRefunds(total)
                .pending(pending)
                .approved(approved)
                .rejected(rejected)
                .completed(completed)
                .totalAmount(totalAmount != null ? totalAmount : BigDecimal.ZERO)
                .pendingAmount(pendingAmount != null ? pendingAmount : BigDecimal.ZERO)
                .approvedAmount(approvedAmount != null ? approvedAmount : BigDecimal.ZERO)
                .completedAmount(completedAmount != null ? completedAmount : BigDecimal.ZERO)
                .byStatus(byStatus)
                .amountByStatus(amountByStatus)
                .build();
    }

    @Override
    public RefundStatsResponse getRefundStatsBySeller(UUID sellerId) {
        List<Object[]> results = refundRepository.sumAmountBySellerId(
                Arrays.asList(RefundStatus.APPROVED, RefundStatus.COMPLETED));

        BigDecimal sellerRefunds = results.stream()
                .filter(r -> r[0].equals(sellerId))
                .map(r -> (BigDecimal) r[1])
                .findFirst()
                .orElse(BigDecimal.ZERO);

        return RefundStatsResponse.builder()
                .totalRefunds(refundRepository.findBySellerId(sellerId, Pageable.unpaged()).getTotalElements())
                .totalAmount(sellerRefunds)
                .build();
    }

    @Override
    public String exportRefundsToCSV(
            RefundStatus status,
            UUID customerId,
            UUID sellerId,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            String query) {
        Page<Refund> refunds = refundRepository.searchRefunds(status, customerId, sellerId, dateFrom, dateTo, query, Pageable.unpaged());

        StringBuilder csv = new StringBuilder();
        csv.append("Refund ID,Order Number,Customer,Amount,Status,Reason,Created At\n");

        refunds.getContent().forEach(refund -> {
            csv.append(String.format("%s,%s,%s,%s,%s,%s,%s\n",
                    refund.getRefundNumber(),
                    refund.getOrder().getOrderNumber(),
                    refund.getCustomerId(),
                    refund.getAmount(),
                    refund.getStatus(),
                    refund.getReason(),
                    refund.getCreatedAt()));
        });

        return csv.toString();
    }

    private RefundResponse mapToResponse(Refund refund) {
        Order order = refund.getOrder();
        
        String customerName = "";
        String customerEmail = "";
        try {
            User customer = order.getCustomer();
            customerName = customer.getFirstName() + " " + customer.getLastName();
            customerEmail = customer.getEmail();
        } catch (Exception e) {
            log.warn("Could not fetch customer details for refund: {}", refund.getId());
        }

        String sellerName = "";
        if (refund.getSellerId() != null) {
            sellerName = refund.getSellerId().toString();
        }

        return RefundResponse.builder()
                .id(refund.getId())
                .refundNumber(refund.getRefundNumber())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(refund.getCustomerId())
                .customerName(customerName)
                .customerEmail(customerEmail)
                .sellerId(refund.getSellerId())
                .sellerName(sellerName)
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .reason(refund.getReason())
                .reasonDisplayName(refund.getReason().getDisplayName())
                .customerNote(refund.getCustomerNote())
                .adminNote(refund.getAdminNote())
                .rejectionReason(refund.getRejectionReason())
                .reviewedAt(refund.getReviewedAt())
                .reviewedBy(refund.getReviewedBy())
                .completedAt(refund.getCompletedAt())
                .transactionId(refund.getTransactionId())
                .createdAt(refund.getCreatedAt())
                .updatedAt(refund.getUpdatedAt())
                .build();
    }
}
