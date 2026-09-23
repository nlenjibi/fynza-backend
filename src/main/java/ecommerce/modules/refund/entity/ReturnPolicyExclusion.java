package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnPolicyExclusionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_policy_exclusions", indexes = {
        @Index(name = "idx_rpe_policy_id",    columnList = "policy_id"),
        @Index(name = "idx_rpe_reference_id", columnList = "reference_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnPolicyExclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "exclusion_type", nullable = false, length = 20)
    private ReturnPolicyExclusionType exclusionType;

    @Column(name = "reference_id", nullable = false)
    private UUID referenceId;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
