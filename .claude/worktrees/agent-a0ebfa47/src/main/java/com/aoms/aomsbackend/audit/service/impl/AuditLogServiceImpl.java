package com.aoms.aomsbackend.audit.service.impl;

import com.aoms.aomsbackend.audit.dto.AuditLogEntry;
import com.aoms.aomsbackend.audit.event.OmsAuditEvent;
import com.aoms.aomsbackend.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    public void log(AuditLogEntry entry) {
        OmsAuditEvent event = new OmsAuditEvent(
                UUID.randomUUID(),
                entry.getActorId(),
                entry.getActorRole(),
                entry.getAction(),
                entry.getEntityType(),
                entry.getEntityId(),
                entry.getLocationId(),
                entry.getPreviousState(),
                entry.getNewState(),
                entry.getCorrelationId(),
                Instant.now()
        );
        eventPublisher.publishEvent(event);
    }
}
