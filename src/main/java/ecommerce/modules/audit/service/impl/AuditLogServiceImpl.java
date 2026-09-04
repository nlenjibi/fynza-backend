package ecommerce.modules.audit.service.impl;

import ecommerce.modules.audit.constant.AuditStatus;
import ecommerce.modules.audit.dto.AuditLogEntry;
import ecommerce.modules.audit.entity.AuditLog;
import ecommerce.modules.audit.event.FynzaAuditEvent;
import ecommerce.modules.audit.repository.AuditLogRepository;
import ecommerce.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final ApplicationEventPublisher eventPublisher;
    private final AuditLogRepository        auditLogRepository;

    @Override
    public void log(AuditLogEntry entry) {
        eventPublisher.publishEvent(new FynzaAuditEvent(
                UUID.randomUUID(),
                entry.getActorPublicId(),
                entry.getActorEmail(),
                entry.getActorRole(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityPublicId(),
                entry.getPreviousState(),
                entry.getNewState(),
                entry.getReason(),
                entry.getIpAddress(),
                entry.getCorrelationId(),
                Instant.now(),
                entry.getStatus()
        ));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logImmediately(AuditLogEntry entry) {
        auditLogRepository.save(AuditLog.builder()
                .actorPublicId(entry.getActorPublicId())
                .actorEmail(entry.getActorEmail())
                .actorRole(entry.getActorRole())
                .action(entry.getAction())
                .entityType(entry.getEntityType())
                .entityPublicId(entry.getEntityPublicId())
                .previousState(entry.getPreviousState())
                .newState(entry.getNewState())
                .reason(entry.getReason())
                .ipAddress(entry.getIpAddress())
                .correlationId(entry.getCorrelationId())
                .occurredAt(Instant.now())
                .status(AuditStatus.from(entry.getStatus()))
                .build());
    }
}
