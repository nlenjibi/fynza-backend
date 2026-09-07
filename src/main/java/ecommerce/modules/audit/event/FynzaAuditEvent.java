package ecommerce.modules.audit.event;

import ecommerce.common.enums.Role;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Immutable Spring application event representing one auditable operation.
 * Published via {@link org.springframework.context.ApplicationEventPublisher}
 * and persisted by {@link AuditEventListener} after the triggering transaction commits.
 */
public record FynzaAuditEvent(
        UUID   eventId,
        UUID   actorPublicId,
        String actorEmail,
        Role   actorRole,
        String action,
        String entityType,
        UUID   entityPublicId,
        Map<String, Object> previousState,
        Map<String, Object> newState,
        String reason,
        String ipAddress,
        UUID   correlationId,
        Instant occurredAt,
        String status
) {}
