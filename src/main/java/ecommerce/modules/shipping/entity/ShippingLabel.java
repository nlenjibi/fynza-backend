package ecommerce.modules.shipping.entity;

import ecommerce.modules.shipping.enums.LabelFormat;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipping_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "shipment")
public class ShippingLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false, unique = true)
    private Shipment shipment;

    @Column(name = "label_url", nullable = false, length = 500)
    private String labelUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", nullable = false, length = 10)
    @Builder.Default
    private LabelFormat format = LabelFormat.PDF;

    @Column(name = "carrier_label_id", length = 200)
    private String carrierLabelId;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
    }
}
