package ecommerce.modules.audit.event;

import ecommerce.modules.audit.constant.AuditStatus;
import ecommerce.modules.audit.entity.AuditLog;
import ecommerce.modules.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Persists {@link FynzaAuditEvent} instances into the audit_log table.
 *
 * Runs AFTER_COMMIT (with fallback) so the audit record is never lost when
 * the publishing transaction is rolled back. REQUIRES_NEW ensures the write
 * commits independently — a listener failure never rolls back the original work.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventListener {

    private final AuditLogRepository auditLogRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditEvent(FynzaAuditEvent event) {
        try {
            auditLogRepository.save(toEntity(event));
        } catch (Exception ex) {
            log.error("[AuditLog] Failed to persist audit event {} action={}: {}",
                    event.eventId(), event.action(), ex.getMessage(), ex);
        }
    }

    private AuditLog toEntity(FynzaAuditEvent e) {
        return AuditLog.builder()
                .actorPublicId(e.actorPublicId())
                .actorEmail(e.actorEmail())
                .actorRole(e.actorRole())
                .action(e.action())
                .entityType(e.entityType())
                .entityPublicId(e.entityPublicId())
                .previousState(e.previousState())
                .newState(e.newState())
                .reason(e.reason())
                .ipAddress(e.ipAddress())
                .occurredAt(e.occurredAt())
                .correlationId(e.correlationId())
                .status(AuditStatus.from(e.status()))
                .build();
    }
}
