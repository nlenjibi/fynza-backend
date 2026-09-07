package ecommerce.modules.audit.dto;

import ecommerce.common.enums.Role;
import ecommerce.modules.audit.constant.AuditStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

/**
 * Immutable value object carrying everything needed to write one audit log row.
 * Build via the Lombok builder and pass to {@link ecommerce.modules.audit.service.AuditLogService}.
 *
 * Use {@link #STATUS_SUCCESS} / {@link #STATUS_FAILURE} for the status field.
 */
@Getter
@Builder
public class AuditLogEntry {

    public static final String STATUS_SUCCESS = AuditStatus.SUCCESS.name();
    public static final String STATUS_FAILURE = AuditStatus.FAILED.name();

    private final UUID   actorPublicId;
    private final String actorEmail;
    private final Role   actorRole;
    private final String action;
    private final String entityType;
    private final UUID   entityPublicId;
    private final Map<String, Object> previousState;
    private final Map<String, Object> newState;
    private final String reason;
    private final String ipAddress;
    private final UUID   correlationId;
    private final String status;
}
