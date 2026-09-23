package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnFraudSignalType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_fraud_signals", indexes = {
        @Index(name = "idx_return_fraud_signals_return_id",   columnList = "return_id"),
        @Index(name = "idx_return_fraud_signals_customer_id", columnList = "customer_id"),
        @Index(name = "idx_return_fraud_signals_created_at",  columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnFraudSignal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "signal_type", nullable = false, length = 40)
    private ReturnFraudSignalType signalType;

    @Column(name = "description", length = 500)
    private String description;

    /** Risk severity 1 (low) – 10 (high). Severity ≥ 7 triggers auto-escalation. */
    @Column(name = "severity", nullable = false)
    private Integer severity;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        detectedAt = Instant.now();
        createdAt  = Instant.now();
    }
}
