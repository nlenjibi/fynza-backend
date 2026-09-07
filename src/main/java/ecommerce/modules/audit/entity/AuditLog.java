package ecommerce.modules.audit.entity;

import ecommerce.common.enums.Role;
import ecommerce.modules.audit.constant.AuditStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Append-only audit trail entry. Does NOT extend BaseEntity — it is never soft-deleted,
 * has no publicId (its own UUID is already the stable identifier), and timestamps are
 * stored as Instant to match wall-clock precision.
 *
 * Captures: who (actorPublicId, actorRole, actorEmail), what (action, entityType,
 * entityPublicId), state (previousState, newState), and context (ipAddress, correlationId).
 */
@Entity
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_actor",       columnList = "actor_public_id"),
        @Index(name = "idx_audit_entity",      columnList = "entity_type, entity_public_id"),
        @Index(name = "idx_audit_action",      columnList = "action"),
        @Index(name = "idx_audit_occurred_at", columnList = "occurred_at"),
        @Index(name = "idx_audit_status",      columnList = "status")
})
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "actor_public_id", nullable = false, columnDefinition = "UUID")
    private UUID actorPublicId;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", nullable = false, length = 30)
    private Role actorRole;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_public_id", columnDefinition = "UUID")
    private UUID entityPublicId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_state", columnDefinition = "jsonb")
    private Map<String, Object> previousState;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_state", columnDefinition = "jsonb")
    private Map<String, Object> newState;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "correlation_id", columnDefinition = "UUID")
    private UUID correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 10)
    private AuditStatus status;
}
