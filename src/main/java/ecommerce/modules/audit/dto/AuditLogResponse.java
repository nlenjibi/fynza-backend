package ecommerce.modules.audit.dto;

import ecommerce.common.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

/**
 * Summary row for paginated audit log list. Full state snapshots and diffs
 * are in {@link AuditLogDetailResponse}.
 */
@Getter
@Builder
public class AuditLogResponse {
    private UUID          id;
    private UUID          actorPublicId;
    private AuditActorInfo actor;
    private Role          actorRole;
    private String        action;
    private String        entityType;
    private UUID          entityPublicId;
    private String        ipAddress;
    private Instant       occurredAt;
    private UUID          correlationId;
    private String        status;
}
