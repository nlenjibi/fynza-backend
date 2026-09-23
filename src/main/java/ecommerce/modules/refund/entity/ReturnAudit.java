package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnAuditAction;
import ecommerce.modules.refund.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_audit", indexes = {
        @Index(name = "idx_return_audit_return_id",   columnList = "return_id"),
        @Index(name = "idx_return_audit_created_at",  columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private ReturnAuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 30)
    private ReturnStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", length = 30)
    private ReturnStatus newStatus;

    @Column(name = "performed_by")
    private UUID performedBy;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
