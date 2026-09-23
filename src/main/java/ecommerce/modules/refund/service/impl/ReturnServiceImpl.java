package ecommerce.modules.refund.service.impl;

import ecommerce.common.enums.OrderStatus;
import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.OrderItem;
import ecommerce.modules.order.repository.OrderItemRepository;
import ecommerce.modules.order.repository.OrderRepository;
import ecommerce.modules.refund.ReturnStateMachine;
import ecommerce.modules.refund.dto.*;
import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.entity.ReturnItem;
import ecommerce.modules.refund.ReturnAuditRecorder;
import ecommerce.modules.refund.entity.ReturnPolicy;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnReason;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.exception.ReturnNotEligibleException;
import ecommerce.modules.refund.exception.ReturnNotFoundException;
import ecommerce.modules.refund.repository.ReturnItemRepository;
import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnPolicyService;
import ecommerce.modules.refund.service.ReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReturnServiceImpl implements ReturnService {

    private static final List<ReturnStatus> TERMINAL_STATUSES =
            Arrays.asList(ReturnStatus.CANCELLED, ReturnStatus.REJECTED, ReturnStatus.EXPIRED, ReturnStatus.RESOLVED);

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ReturnPolicyService returnPolicyService;
    private final ReturnAuditRecorder auditRecorder;

    @Override
    public ReturnEligibilityResult checkEligibility(UUID orderId, UUID customerId) {
        Order order = orderRepository.findByPublicId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (!order.getCustomerId().equals(customerId)) {
            return ReturnEligibilityResult.builder()
                    .eligible(false)
                    .reason("Order does not belong to this customer")
                    .build();
        }

        if (order.getStatus() != OrderStatus.DELIVERED) {
            return ReturnEligibilityResult.builder()
                    .eligible(false)
                    .reason("Order must be delivered before a return can be requested")
                    .build();
        }

        ReturnPolicy policy = returnPolicyService.resolvePolicy(null, null, null);

        if (Boolean.FALSE.equals(policy.getIsReturnable())) {
            return ReturnEligibilityResult.builder()
                    .eligible(false)
                    .reason("Returns are not permitted under the current policy")
                    .build();
        }

        Instant deliveredAt = order.getUpdatedAt();
        Instant deadline = deliveredAt.plus(policy.getReturnWindowDays(), ChronoUnit.DAYS);
        long remainingDays = ChronoUnit.DAYS.between(Instant.now(), deadline);

        if (remainingDays < 0) {
            return ReturnEligibilityResult.builder()
                    .eligible(false)
                    .deadline(deadline)
                    .remainingDays(0L)
                    .reason("Return window of " + policy.getReturnWindowDays() + " days has expired")
                    .build();
        }

        boolean hasActiveReturn = returnRepository.existsByOrderIdAndStatusNotIn(orderId, TERMINAL_STATUSES);
        if (hasActiveReturn) {
            return ReturnEligibilityResult.builder()
                    .eligible(false)
                    .deadline(deadline)
                    .remainingDays(remainingDays)
                    .reason("An active return request already exists for this order")
                    .build();
        }

        List<ReturnReason> allowed = resolveAllowedReasons(policy);
        return ReturnEligibilityResult.builder()
                .eligible(true)
                .deadline(deadline)
                .remainingDays(remainingDays)
                .allowedReasons(allowed)
                .build();
    }

    @Override
    @Transactional
    public ReturnResponse createReturn(CreateReturnRequest request, UUID customerId) {
        ReturnEligibilityResult eligibility = checkEligibility(request.getOrderId(), customerId);
        if (!eligibility.isEligible()) {
            throw new ReturnNotEligibleException(eligibility.getReason());
        }

        if (eligibility.getAllowedReasons() != null
                && !eligibility.getAllowedReasons().contains(request.getReason())) {
            throw new ReturnNotEligibleException(
                    "Reason " + request.getReason() + " is not permitted under the current return policy");
        }

        String returnNumber = Return.generateReturnNumber();
        while (returnRepository.findByReturnNumber(returnNumber).isPresent()) {
            returnNumber = Return.generateReturnNumber();
        }

        Return returnEntity = Return.builder()
                .returnNumber(returnNumber)
                .orderId(request.getOrderId())
                .customerId(customerId)
                .status(ReturnStatus.REQUESTED)
                .reason(request.getReason())
                .customerNote(request.getCustomerNote())
                .returnDeadline(eligibility.getDeadline())
                .build();

        returnEntity = returnRepository.save(returnEntity);

        final UUID returnPublicId = returnEntity.getPublicId();
        List<ReturnItem> items = buildReturnItems(request.getItems(), returnPublicId);
        returnItemRepository.saveAll(items);

        auditRecorder.record(returnPublicId, ReturnAuditAction.CREATED,
                null, ReturnStatus.REQUESTED, customerId, "Return request submitted");

        log.info("Return {} created for order {} by customer {}", returnNumber, request.getOrderId(), customerId);
        return toResponse(returnEntity, items);
    }

    @Override
    public ReturnResponse getReturn(UUID returnPublicId, UUID requestingUserId) {
        Return returnEntity = findByPublicId(returnPublicId);
        List<ReturnItem> items = returnItemRepository.findByReturnId(returnPublicId);
        return toResponse(returnEntity, items);
    }

    @Override
    public Page<ReturnResponse> getReturnsByCustomer(UUID customerId, Pageable pageable) {
        return returnRepository.findByCustomerId(customerId, pageable)
                .map(r -> toResponse(r, returnItemRepository.findByReturnId(r.getPublicId())));
    }

    @Override
    public List<ReturnResponse> getReturnsByOrder(UUID orderId) {
        return returnRepository.findByOrderId(orderId).stream()
                .map(r -> toResponse(r, returnItemRepository.findByReturnId(r.getPublicId())))
                .collect(Collectors.toList());
    }

    @Override
    public Page<ReturnResponse> getReturnsBySeller(UUID sellerId, Pageable pageable) {
        return returnRepository.findBySellerId(sellerId, pageable)
                .map(r -> toResponse(r, returnItemRepository.findByReturnId(r.getPublicId())));
    }

    @Override
    public Page<ReturnResponse> getAllReturns(Pageable pageable) {
        return returnRepository.findAll(pageable)
                .map(r -> toResponse(r, returnItemRepository.findByReturnId(r.getPublicId())));
    }

    @Override
    @Transactional
    public ReturnResponse cancelReturn(UUID returnPublicId, UUID customerId) {
        Return returnEntity = findByPublicId(returnPublicId);

        if (!returnEntity.getCustomerId().equals(customerId)) {
            throw new ReturnNotEligibleException("You do not own this return request");
        }

        ReturnStatus prev = returnEntity.getStatus();
        ReturnStateMachine.validate(prev, ReturnStatus.CANCELLED);
        returnEntity.setStatus(ReturnStatus.CANCELLED);
        returnEntity = returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.CANCELLED,
                prev, ReturnStatus.CANCELLED, customerId, "Cancelled by customer");

        log.info("Return {} cancelled by customer {}", returnEntity.getReturnNumber(), customerId);
        return toResponse(returnEntity, returnItemRepository.findByReturnId(returnPublicId));
    }

    @Override
    @Transactional
    public ReturnResponse approveReturn(UUID returnPublicId, UUID approvedBy, String adminNote) {
        Return returnEntity = findByPublicId(returnPublicId);
        ReturnStatus prev = returnEntity.getStatus();
        ReturnStateMachine.validate(prev, ReturnStatus.APPROVED);
        returnEntity.setStatus(ReturnStatus.APPROVED);
        returnEntity.setApprovedAt(Instant.now());
        returnEntity.setAdminNote(adminNote);
        returnEntity = returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.APPROVED,
                prev, ReturnStatus.APPROVED, approvedBy, adminNote);

        log.info("Return {} approved by {}", returnEntity.getReturnNumber(), approvedBy);
        return toResponse(returnEntity, returnItemRepository.findByReturnId(returnPublicId));
    }

    @Override
    @Transactional
    public ReturnResponse rejectReturn(UUID returnPublicId, UUID rejectedBy, String rejectionReason) {
        Return returnEntity = findByPublicId(returnPublicId);
        ReturnStatus prev = returnEntity.getStatus();
        ReturnStateMachine.validate(prev, ReturnStatus.REJECTED);
        returnEntity.setStatus(ReturnStatus.REJECTED);
        returnEntity.setRejectedAt(Instant.now());
        returnEntity.setRejectionReason(rejectionReason);
        returnEntity = returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.REJECTED,
                prev, ReturnStatus.REJECTED, rejectedBy, rejectionReason);

        log.info("Return {} rejected by {}", returnEntity.getReturnNumber(), rejectedBy);
        return toResponse(returnEntity, returnItemRepository.findByReturnId(returnPublicId));
    }

    @Override
    @Transactional
    public ReturnResponse markUnderReview(UUID returnPublicId, UUID reviewedBy) {
        Return returnEntity = findByPublicId(returnPublicId);
        ReturnStatus prev = returnEntity.getStatus();
        ReturnStateMachine.validate(prev, ReturnStatus.UNDER_REVIEW);
        returnEntity.setStatus(ReturnStatus.UNDER_REVIEW);
        returnEntity = returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.REVIEW_STARTED,
                prev, ReturnStatus.UNDER_REVIEW, reviewedBy, "Review started");

        log.info("Return {} moved to UNDER_REVIEW by {}", returnEntity.getReturnNumber(), reviewedBy);
        return toResponse(returnEntity, returnItemRepository.findByReturnId(returnPublicId));
    }

    @Override
    @Transactional
    public ReturnResponse escalateReturn(UUID returnPublicId, UUID escalatedBy, String reason) {
        Return returnEntity = findByPublicId(returnPublicId);
        returnEntity.setIsEscalated(true);
        returnEntity.setEscalatedAt(Instant.now());
        returnEntity = returnRepository.save(returnEntity);

        auditRecorder.record(returnPublicId, ReturnAuditAction.ESCALATED,
                returnEntity.getStatus(), returnEntity.getStatus(), escalatedBy, reason);

        log.info("Return {} escalated by {} — {}", returnEntity.getReturnNumber(), escalatedBy, reason);
        return toResponse(returnEntity, returnItemRepository.findByReturnId(returnPublicId));
    }

    private Return findByPublicId(UUID publicId) {
        return returnRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ReturnNotFoundException("Return not found: " + publicId));
    }

    private List<ReturnItem> buildReturnItems(List<ReturnItemRequest> requests, UUID returnPublicId) {
        return requests.stream().map(req -> {
            OrderItem orderItem = orderItemRepository.findByPublicId(req.getOrderItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order item not found: " + req.getOrderItemId()));
            return ReturnItem.builder()
                    .returnId(returnPublicId)
                    .orderItemId(req.getOrderItemId())
                    .productId(orderItem.getProductId())
                    .productName(orderItem.getProductName())
                    .quantity(req.getQuantity())
                    .reason(req.getReason())
                    .condition(req.getCondition())
                    .unitPrice(orderItem.getUnitPrice())
                    .build();
        }).collect(Collectors.toList());
    }

    private ReturnResponse toResponse(Return r, List<ReturnItem> items) {
        return ReturnResponse.builder()
                .publicId(r.getPublicId())
                .returnNumber(r.getReturnNumber())
                .orderId(r.getOrderId())
                .customerId(r.getCustomerId())
                .sellerId(r.getSellerId())
                .storeId(r.getStoreId())
                .status(r.getStatus())
                .reason(r.getReason())
                .customerNote(r.getCustomerNote())
                .adminNote(r.getAdminNote())
                .rejectionReason(r.getRejectionReason())
                .returnDeadline(r.getReturnDeadline())
                .requestedAt(r.getRequestedAt())
                .approvedAt(r.getApprovedAt())
                .receivedAt(r.getReceivedAt())
                .rejectedAt(r.getRejectedAt())
                .resolvedAt(r.getResolvedAt())
                .isEscalated(r.getIsEscalated())
                .escalatedAt(r.getEscalatedAt())
                .items(items.stream().map(this::toItemResponse).collect(Collectors.toList()))
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }

    private List<ReturnReason> resolveAllowedReasons(ReturnPolicy policy) {
        String serialized = policy.getEligibleReasons();
        if (serialized == null || serialized.isBlank()) return List.of(ReturnReason.values());
        return Arrays.stream(serialized.split(","))
                .map(String::trim)
                .map(ReturnReason::valueOf)
                .collect(Collectors.toList());
    }

    private ReturnItemResponse toItemResponse(ReturnItem i) {
        return ReturnItemResponse.builder()
                .publicId(i.getPublicId())
                .orderItemId(i.getOrderItemId())
                .productId(i.getProductId())
                .productName(i.getProductName())
                .quantity(i.getQuantity())
                .reason(i.getReason())
                .condition(i.getCondition())
                .unitPrice(i.getUnitPrice())
                .createdAt(i.getCreatedAt())
                .build();
    }
}
