package ecommerce.modules.refund.service.impl;

import ecommerce.common.exception.ResourceNotFoundException;
import ecommerce.modules.refund.ReturnAuditRecorder;
import ecommerce.modules.refund.dto.ReturnFraudSignalResponse;
import ecommerce.modules.refund.entity.Return;
import ecommerce.modules.refund.entity.ReturnFraudSignal;
import ecommerce.modules.refund.entity.ReturnItem;
import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnFraudSignalType;
import ecommerce.modules.refund.enums.ReturnItemCondition;
import ecommerce.modules.refund.repository.ReturnFraudSignalRepository;
import ecommerce.modules.refund.repository.ReturnItemRepository;
import ecommerce.modules.refund.repository.ReturnRepository;
import ecommerce.modules.refund.service.ReturnFraudSignalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReturnFraudSignalServiceImpl implements ReturnFraudSignalService {

    private static final int HIGH_RETURN_RATE_THRESHOLD = 5;
    private static final int DAMAGE_CLAIM_THRESHOLD = 3;
    private static final BigDecimal HIGH_VALUE_THRESHOLD = new BigDecimal("2000.00");
    private static final int AUTO_ESCALATION_SEVERITY = 7;

    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final ReturnFraudSignalRepository fraudSignalRepository;
    private final ReturnAuditRecorder auditRecorder;

    @Override
    public void analyzeAndRecord(Return ret) {
        List<ReturnFraudSignal> signals = new ArrayList<>();
        Instant now = Instant.now();

        long recentReturns = fraudSignalRepository.countByCustomerIdAndCreatedAtAfter(
                ret.getCustomerId(), now.minus(30, ChronoUnit.DAYS));
        if (recentReturns > HIGH_RETURN_RATE_THRESHOLD) {
            signals.add(buildSignal(ret, ReturnFraudSignalType.HIGH_RETURN_RATE,
                    "Customer has " + recentReturns + " returns in the last 30 days", 6));
        }

        long damageClaims = fraudSignalRepository.countByCustomerIdAndSignalTypeAndCreatedAtAfter(
                ret.getCustomerId(), ReturnFraudSignalType.REPEATED_DAMAGE_CLAIM,
                now.minus(90, ChronoUnit.DAYS));
        List<ReturnItem> items = returnItemRepository.findByReturnId(ret.getPublicId());
        boolean hasDamageClaim = items.stream().anyMatch(i ->
                i.getCondition() == ReturnItemCondition.DAMAGED ||
                i.getCondition() == ReturnItemCondition.DEFECTIVE);
        if (hasDamageClaim && damageClaims >= DAMAGE_CLAIM_THRESHOLD) {
            signals.add(buildSignal(ret, ReturnFraudSignalType.REPEATED_DAMAGE_CLAIM,
                    "Customer has " + (damageClaims + 1) + " damage/defect claims in last 90 days", 7));
        }

        BigDecimal totalValue = items.stream()
                .filter(i -> i.getUnitPrice() != null)
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalValue.compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            signals.add(buildSignal(ret, ReturnFraudSignalType.HIGH_VALUE_RETURN,
                    "Return value GHS " + totalValue + " exceeds threshold", 5));
        }

        if (!signals.isEmpty()) {
            fraudSignalRepository.saveAll(signals);
            boolean needsEscalation = signals.stream()
                    .anyMatch(s -> s.getSeverity() >= AUTO_ESCALATION_SEVERITY);
            if (needsEscalation && !Boolean.TRUE.equals(ret.getIsEscalated())) {
                ret.setIsEscalated(true);
                ret.setEscalatedAt(now);
                returnRepository.save(ret);
            }
            auditRecorder.record(ret.getPublicId(), ReturnAuditAction.FRAUD_SIGNAL_DETECTED,
                    null, "Detected " + signals.size() + " fraud signal(s)");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReturnFraudSignalResponse> getSignals(UUID returnPublicId) {
        Return ret = returnRepository.findByPublicId(returnPublicId)
                .orElseThrow(() -> new ResourceNotFoundException("Return not found: " + returnPublicId));
        return fraudSignalRepository.findByReturnIdOrderByCreatedAtDesc(ret.getPublicId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private ReturnFraudSignal buildSignal(Return ret, ReturnFraudSignalType type,
                                          String description, int severity) {
        return ReturnFraudSignal.builder()
                .returnId(ret.getPublicId())
                .customerId(ret.getCustomerId())
                .signalType(type)
                .description(description)
                .severity(severity)
                .build();
    }

    private ReturnFraudSignalResponse toResponse(ReturnFraudSignal s) {
        return ReturnFraudSignalResponse.builder()
                .id(s.getId())
                .returnId(s.getReturnId())
                .customerId(s.getCustomerId())
                .signalType(s.getSignalType())
                .description(s.getDescription())
                .severity(s.getSeverity())
                .detectedAt(s.getDetectedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
