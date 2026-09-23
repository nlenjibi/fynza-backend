package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.InspectionCondition;
import ecommerce.modules.refund.enums.InspectionResult;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_inspections", indexes = {
        @Index(name = "idx_return_inspections_return_id",      columnList = "return_id"),
        @Index(name = "idx_return_inspections_return_item_id", columnList = "return_item_id"),
        @Index(name = "idx_return_inspections_created_at",     columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnInspection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "return_item_id")
    private UUID returnItemId;

    @Column(name = "inspected_by")
    private UUID inspectedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "condition", nullable = false, length = 20)
    private InspectionCondition condition;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private InspectionResult result;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "inspected_at", nullable = false)
    private Instant inspectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId    = UUID.randomUUID();
        inspectedAt = Instant.now();
        createdAt   = Instant.now();
        updatedAt   = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
