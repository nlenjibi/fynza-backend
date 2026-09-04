package com.aoms.aomsbackend.audit.event;

import com.aoms.aomsbackend.audit.entity.AuditLog;
import com.aoms.aomsbackend.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(OmsAuditEvent event) {
        auditLogRepository.save(toAuditLog(event));
    }

    private AuditLog toAuditLog(OmsAuditEvent event) {
        return AuditLog.builder()
                .actorId(event.actorId())
                .actorRole(event.actorRole())
                .action(event.action())
                .entityType(event.entityType())
                .entityId(event.entityId())
                .locationId(event.locationId())
                .previousState(event.previousState())
                .newState(event.newState())
                .occurredAt(event.occurredAt())
                .correlationId(event.correlationId())
                .build();
    }
}
