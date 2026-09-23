package ecommerce.modules.refund.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.refund.ReturnAuditRecorder;
import ecommerce.modules.refund.dto.RequestExchangeRequest;
import ecommerce.modules.refund.dto.ReturnExchangeResponse;
import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.entity.ReturnExchange;
import ecommerce.modules.refund.enums.ExchangeStatus;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnStatus;
import ecommerce.modules.refund.repository.ReturnExchangeRepository;
import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnExchangeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ReturnExchangeServiceImpl implements ReturnExchangeService {

    private static final Set<ReturnStatus> EXCHANGE_ELIGIBLE = EnumSet.of(
            ReturnStatus.APPROVED, ReturnStatus.INSPECTION, ReturnStatus.APPROVED_FOR_REFUND,
            ReturnStatus.PARTIALLY_APPROVED);

    private final ReturnRepository returnRepository;
    private final ReturnExchangeRepository exchangeRepository;
    private final ReturnAuditRecorder auditRecorder;

    @Override
    public ReturnExchangeResponse requestExchange(UUID returnPublicId, RequestExchangeRequest request,
                                                   UUID requestedBy) {
        Return ret = returnRepository.findByPublicId(returnPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnPublicId));

        if (!EXCHANGE_ELIGIBLE.contains(ret.getStatus())) {
            throw new IllegalStateException(
                    "Exchange not allowed for return in status: " + ret.getStatus());
        }
        if (exchangeRepository.existsByReturnId(ret.getPublicId())) {
            throw new IllegalStateException("Exchange already requested for this return");
        }

        ReturnExchange exchange = ReturnExchange.builder()
                .returnId(ret.getPublicId())
                .originalOrderId(ret.getOrderId())
                .requestedItemsDescription(request.getRequestedItemsDescription())
                .notes(request.getNotes())
                .requestedBy(requestedBy)
                .build();
        exchange = exchangeRepository.save(exchange);

        auditRecorder.record(ret.getPublicId(), ReturnAuditAction.EXCHANGE_REQUESTED,
                requestedBy, "Exchange requested: " + request.getRequestedItemsDescription());

        return toResponse(exchange);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReturnExchangeResponse> getExchange(UUID returnPublicId) {
        Return ret = returnRepository.findByPublicId(returnPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnPublicId));
        return exchangeRepository.findByReturnId(ret.getPublicId()).map(this::toResponse);
    }

    @Override
    public ReturnExchangeResponse updateExchangeStatus(UUID returnPublicId, ExchangeStatus newStatus,
                                                        UUID updatedBy) {
        Return ret = returnRepository.findByPublicId(returnPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnPublicId));
        ReturnExchange exchange = exchangeRepository.findByReturnId(ret.getPublicId())
                .orElseThrow(() -> new ResourceNotFoundException("No exchange for return: " + returnPublicId));

        exchange.setStatus(newStatus);
        if (newStatus == ExchangeStatus.COMPLETED || newStatus == ExchangeStatus.CANCELLED) {
            exchange.setProcessedBy(updatedBy);
            exchange.setProcessedAt(Instant.now());
        }
        exchange = exchangeRepository.save(exchange);
        return toResponse(exchange);
    }

    private ReturnExchangeResponse toResponse(ReturnExchange e) {
        return ReturnExchangeResponse.builder()
                .publicId(e.getPublicId())
                .returnId(e.getReturnId())
                .originalOrderId(e.getOriginalOrderId())
                .exchangeOrderId(e.getExchangeOrderId())
                .status(e.getStatus())
                .requestedItemsDescription(e.getRequestedItemsDescription())
                .notes(e.getNotes())
                .requestedBy(e.getRequestedBy())
                .processedBy(e.getProcessedBy())
                .processedAt(e.getProcessedAt())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
