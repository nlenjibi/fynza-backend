package ecommerce.modules.refund.service.impl;

import ecommerce.modules.payment.service.PaymentRefundService;
import ecommerce.modules.payment.service.RefundExecutionResult;
import ecommerce.modules.refund.ReturnAuditRecorder;
import ecommerce.modules.refund.ReturnStateMachine;
import ecommerce.modules.refund.dto.ReturnReconciliationResponse;
import ecommerce.modules.refund.dto.ReturnRefundResponse;
import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.entity.ReturnItem;
import ecommerce.modules.refund.entity.ReturnPolicy;
import ecommerce.modules.refund.entity.ReturnReconciliation;
import ecommerce.modules.refund.entity.ReturnRefund;
import ecommerce.modules.refund.enums.ReconciliationResult;
import ecommerce.modules.refund.enums.RefundStatus;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.exception.ReturnNotFoundException;
import ecommerce.modules.refund.repository.ReturnItemRepository;
import ecommerce.modules.refund.repository.ReturnReconciliationRepository;
import ecommerce.modules.refund.repository.ReturnRefundRepository;
import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnPolicyService;
import ecommerce.modules.refund.service.ReturnRefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnRefundServiceImpl implements ReturnRefundService {

    private static final String CURRENCY = "GHS";
    private static final List<ReturnStatus> REFUNDABLE_STATUSES =
            Arrays.asList(ReturnStatus.APPROVED_FOR_REFUND, ReturnStatus.PARTIALLY_APPROVED, ReturnStatus.FAILED);

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnRefundRepository returnRefundRepository;
    private final ReturnReconciliationRepository reconciliationRepository;
    private final ReturnPolicyService returnPolicyService;
    private final PaymentRefundService paymentRefundService;
    private final ReturnAuditRecorder auditRecorder;

    @Override
    @Transactional
    public ReturnRefundResponse requestRefund(UUID returnPublicId, UUID requestedBy) {
        Return returnEntity = findByPublicId(returnPublicId);
        ReturnStatus currentStatus = returnEntity.getStatus();

        if (!REFUNDABLE_STATUSES.contains(currentStatus)) {
            throw new IllegalStateException(
                    "Refund can only be requested from APPROVED_FOR_REFUND, PARTIALLY_APPROVED, or FAILED. Current: " + currentStatus);
        }

        // Idempotency: reuse if already succeeded
        Optional<ReturnRefund> existing = returnRefundRepository.findByReturnId(returnPublicId);
        if (existing.isPresent() && existing.get().getStatus() == RefundStatus.SUCCEEDED) {
            return toRefundResponse(existing.get());
        }

        BigDecimal refundAmount = calculateRefundAmount(returnPublicId);

        ReturnRefund refund = existing
                .filter(r -> r.getStatus() == RefundStatus.FAILED)
                .map(r -> { r.setStatus(RefundStatus.PENDING); r.setFailureReason(null); return r; })
                .orElseGet(() -> ReturnRefund.builder()
                        .returnId(returnPublicId)
                        .orderId(returnEntity.getOrderId())
                        .requestedAmount(refundAmount)
                        .currency(CURRENCY)
                        .reason("Return refund for " + returnEntity.getReturnNumber())
                        .status(RefundStatus.PENDING)
                        .build());
        refund = returnRefundRepository.save(refund);

        // Transition return to REFUND_REQUESTED
        ReturnStatus prev = currentStatus;
        if (currentStatus != ReturnStatus.FAILED) {
            ReturnStateMachine.validate(currentStatus, ReturnStatus.REFUND_REQUESTED);
        }
        returnEntity.setStatus(ReturnStatus.REFUND_REQUESTED);
        returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.REFUND_REQUESTED,
                prev, ReturnStatus.REFUND_REQUESTED, requestedBy,
                "Refund requested: " + refundAmount + " " + CURRENCY);

        // Execute payment refund synchronously
        RefundExecutionResult result = paymentRefundService.executeRefund(
                returnEntity.getOrderId(), refundAmount, refund.getReason());

        if (result.isSuccess()) {
            refund.setApprovedAmount(result.getRefundedAmount() != null ? result.getRefundedAmount() : refundAmount);
            refund.setProviderRefundId(result.getProviderRefundId());
            refund.setProviderReference(result.getProviderReference());
            refund.setStatus(RefundStatus.SUCCEEDED);
            refund.setApprovedAt(Instant.now());
            refund.setProcessedAt(Instant.now());
            refund = returnRefundRepository.save(refund);

            ReturnStateMachine.validate(ReturnStatus.REFUND_REQUESTED, ReturnStatus.RESOLVED);
            returnEntity.setStatus(ReturnStatus.RESOLVED);
            returnEntity.setResolvedAt(Instant.now());
            returnRepository.save(returnEntity);

            auditRecorder.record(returnPublicId, ReturnAuditAction.REFUND_SUCCEEDED,
                    ReturnStatus.REFUND_REQUESTED, ReturnStatus.RESOLVED, requestedBy,
                    "Refund succeeded: " + refund.getApprovedAmount() + " " + CURRENCY
                            + (result.getProviderRefundId() != null ? " [" + result.getProviderRefundId() + "]" : ""));

            log.info("Return {} refund succeeded, providerRefundId={}", returnEntity.getReturnNumber(), result.getProviderRefundId());

        } else {
            refund.setStatus(RefundStatus.FAILED);
            refund.setFailureReason(result.getFailureReason());
            refund = returnRefundRepository.save(refund);

            ReturnStateMachine.validate(ReturnStatus.REFUND_REQUESTED, ReturnStatus.FAILED);
            returnEntity.setStatus(ReturnStatus.FAILED);
            returnRepository.save(returnEntity);

            auditRecorder.record(returnPublicId, ReturnAuditAction.REFUND_FAILED,
                    ReturnStatus.REFUND_REQUESTED, ReturnStatus.FAILED, requestedBy,
                    "Refund failed: " + result.getFailureReason());

            log.warn("Return {} refund failed — {}", returnEntity.getReturnNumber(), result.getFailureReason());
        }

        return toRefundResponse(refund);
    }

    @Override
    public Optional<ReturnRefundResponse> getRefund(UUID returnPublicId) {
        return returnRefundRepository.findByReturnId(returnPublicId).map(this::toRefundResponse);
    }

    @Override
    @Transactional
    public ReturnReconciliationResponse reconcile(UUID returnPublicId, UUID reconciledBy) {
        Return returnEntity = findByPublicId(returnPublicId);
        Optional<ReturnRefund> refundOpt = returnRefundRepository.findByReturnId(returnPublicId);

        ReconciliationResult result = determineReconciliationResult(returnEntity, refundOpt.orElse(null));

        ReturnReconciliation reconciliation = ReturnReconciliation.builder()
                .returnId(returnPublicId)
                .returnStatus(returnEntity.getStatus())
                .shipmentStatus(returnEntity.getReturnShipmentId() != null ? "LINKED" : null)
                .refundStatus(refundOpt.map(ReturnRefund::getStatus).orElse(null))
                .result(result)
                .resolvedBy(result == ReconciliationResult.MATCHED ? reconciledBy : null)
                .resolvedAt(result == ReconciliationResult.MATCHED ? Instant.now() : null)
                .build();
        reconciliation = reconciliationRepository.save(reconciliation);

        log.info("Return {} reconciliation: {}", returnEntity.getReturnNumber(), result);
        return toReconciliationResponse(reconciliation);
    }

    @Override
    public List<ReturnReconciliationResponse> getReconciliations(UUID returnPublicId) {
        return reconciliationRepository.findByReturnIdOrderByCreatedAtDesc(returnPublicId)
                .stream().map(this::toReconciliationResponse).collect(Collectors.toList());
    }

    private ReconciliationResult determineReconciliationResult(Return r, ReturnRefund refund) {
        ReturnStatus status = r.getStatus();
        if (status == ReturnStatus.RESOLVED && refund != null && refund.getStatus() == RefundStatus.SUCCEEDED) {
            return ReconciliationResult.MATCHED;
        }
        if ((status == ReturnStatus.APPROVED_FOR_REFUND || status == ReturnStatus.PARTIALLY_APPROVED)
                && refund == null) {
            return ReconciliationResult.REFUND_MISSING;
        }
        if (status == ReturnStatus.APPROVED && r.getReturnShipmentId() == null) {
            return ReconciliationResult.SHIPMENT_MISSING;
        }
        if (status == ReturnStatus.RESOLVED && (refund == null || refund.getStatus() != RefundStatus.SUCCEEDED)) {
            return ReconciliationResult.STATUS_MISMATCH;
        }
        return ReconciliationResult.UNKNOWN;
    }

    private BigDecimal calculateRefundAmount(UUID returnPublicId) {
        List<ReturnItem> items = returnItemRepository.findByReturnId(returnPublicId);
        ReturnPolicy policy = returnPolicyService.resolvePolicy(null, null, null);
        BigDecimal restockingFeePercent = policy.getRestockingFeePercent() != null
                ? policy.getRestockingFeePercent() : BigDecimal.ZERO;

        BigDecimal itemTotal = items.stream()
                .filter(i -> i.getUnitPrice() != null)
                .map(i -> {
                    int qty = i.getApprovedQuantity() != null ? i.getApprovedQuantity() : i.getQuantity();
                    return i.getUnitPrice().multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal fee = itemTotal.multiply(restockingFeePercent)
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        return itemTotal.subtract(fee).setScale(4, RoundingMode.HALF_UP);
    }

    private Return findByPublicId(UUID publicId) {
        return returnRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ReturnNotFoundException("Return not found: " + publicId));
    }

    private ReturnRefundResponse toRefundResponse(ReturnRefund r) {
        return ReturnRefundResponse.builder()
                .publicId(r.getPublicId())
                .returnId(r.getReturnId())
                .orderId(r.getOrderId())
                .paymentId(r.getPaymentId())
                .requestedAmount(r.getRequestedAmount())
                .approvedAmount(r.getApprovedAmount())
                .currency(r.getCurrency())
                .reason(r.getReason())
                .status(r.getStatus())
                .providerRefundId(r.getProviderRefundId())
                .providerReference(r.getProviderReference())
                .failureReason(r.getFailureReason())
                .requestedAt(r.getRequestedAt())
                .approvedAt(r.getApprovedAt())
                .processedAt(r.getProcessedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReturnReconciliationResponse toReconciliationResponse(ReturnReconciliation r) {
        return ReturnReconciliationResponse.builder()
                .id(r.getId())
                .returnId(r.getReturnId())
                .returnStatus(r.getReturnStatus())
                .shipmentStatus(r.getShipmentStatus())
                .refundStatus(r.getRefundStatus())
                .inventoryStatus(r.getInventoryStatus())
                .result(r.getResult())
                .resolvedBy(r.getResolvedBy())
                .resolvedAt(r.getResolvedAt())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
