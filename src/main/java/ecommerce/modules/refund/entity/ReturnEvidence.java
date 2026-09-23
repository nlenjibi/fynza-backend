package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnEvidenceType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_evidence", indexes = {
        @Index(name = "idx_return_evidence_return_id",      columnList = "return_id"),
        @Index(name = "idx_return_evidence_return_item_id", columnList = "return_item_id"),
        @Index(name = "idx_return_evidence_created_at",     columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnEvidence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "return_item_id")
    private UUID returnItemId;

    @Column(name = "media_reference", nullable = false, length = 1000)
    private String mediaReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 20)
    private ReturnEvidenceType evidenceType;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
    }
}
