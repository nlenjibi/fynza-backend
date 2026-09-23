package ecommerce.modules.shipping.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "shipping_reconciliation", indexes = {
        @Index(name = "idx_shipping_reconciliation_shipment_id", columnList = "shipment_id"),
        @Index(name = "idx_shipping_reconciliation_date", columnList = "reconciliation_date"),
        @Index(name = "idx_shipping_reconciliation_resolved", columnList = "resolved")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ShippingReconciliation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "shipment_id", nullable = false)
    private UUID shipmentId;

    @Column(name = "carrier", length = 50)
    private String carrier;

    @Column(name = "carrier_tracking_number", length = 200)
    private String carrierTrackingNumber;

    @Column(name = "reconciliation_date", nullable = false)
    private LocalDate reconciliationDate;

    @Column(name = "expected_status", length = 30)
    private String expectedStatus;

    @Column(name = "actual_status", length = 30)
    private String actualStatus;

    @Column(name = "discrepancy_type", length = 50)
    private String discrepancyType;

    @Column(name = "discrepancy_notes", columnDefinition = "TEXT")
    private String discrepancyNotes;

    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        if (reconciliationDate == null) {
            reconciliationDate = LocalDate.now();
        }
    }
}
