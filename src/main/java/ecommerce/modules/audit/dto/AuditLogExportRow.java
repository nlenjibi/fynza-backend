package ecommerce.modules.audit.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Narrow projection used by the CSV export; avoids hydrating full AuditLog entities
 * and the JSONB state columns.
 */
public record AuditLogExportRow(
        UUID    id,
        Instant occurredAt,
        String  actorEmail,
        String  actorRole,
        String  action,
        String  entityType,
        String  ipAddress,
        String  reason,
        String  status
) {}
