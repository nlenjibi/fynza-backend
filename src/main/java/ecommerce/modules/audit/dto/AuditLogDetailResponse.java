package ecommerce.modules.audit.dto;

import ecommerce.common.enums.Role;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Full detail for a single audit log entry, including computed field-level diff.
 */
@Getter
@Builder
public class AuditLogDetailResponse {
    private UUID          id;
    private UUID          actorPublicId;
    private AuditActorInfo actor;
    private Role          actorRole;
    private String        action;
    private String        entityType;
    private UUID          entityPublicId;
    private Map<String, Object> previousState;
    private Map<String, Object> newState;
    private Map<String, FieldDiff> diff;
    private String        reason;
    private String        ipAddress;
    private Instant       occurredAt;
    private UUID          correlationId;
    private String        status;

    /** Before/after values for a single field that changed between two states. */
    @Getter
    @Builder
    public static class FieldDiff {
        private Object before;
        private Object after;
    }
}
