package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReconciliationResult;
import ecommerce.modules.refund.enums.RefundStatus;
import ecommerce.modules.refund.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_reconciliations", indexes = {
        @Index(name = "idx_return_reconciliations_return_id",  columnList = "return_id"),
        @Index(name = "idx_return_reconciliations_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Enumerated(EnumType.STRING)
    @Column(name = "return_status", length = 30)
    private ReturnStatus returnStatus;

    @Column(name = "shipment_status", length = 30)
    private String shipmentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_status", length = 30)
    private RefundStatus refundStatus;

    @Column(name = "inventory_status", length = 30)
    private String inventoryStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 30)
    private ReconciliationResult result;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
