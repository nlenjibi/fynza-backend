package ecommerce.modules.refund.entity;

import ecommerce.modules.refund.enums.ReturnDispositionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "return_dispositions", indexes = {
        @Index(name = "idx_return_dispositions_return_id",      columnList = "return_id"),
        @Index(name = "idx_return_dispositions_return_item_id", columnList = "return_item_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ReturnDisposition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "return_id", nullable = false)
    private UUID returnId;

    @Column(name = "return_item_id", nullable = false)
    private UUID returnItemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ReturnDispositionType type;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "location_id", length = 100)
    private String locationId;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "processed_by")
    private UUID processedBy;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        publicId    = UUID.randomUUID();
        processedAt = Instant.now();
        createdAt   = Instant.now();
    }
}
