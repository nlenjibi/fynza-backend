package ecommerce.modules.customer.entity;

import ecommerce.modules.customer.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers", indexes = {
        @Index(name = "idx_customers_user_id",   columnList = "user_id"),
        @Index(name = "idx_customers_status",    columnList = "status"),
        @Index(name = "idx_customers_number",    columnList = "customer_number"),
        @Index(name = "idx_customers_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(of = {"id", "publicId", "customerNumber", "status"})
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "customer_number", nullable = false, unique = true, length = 20)
    private String customerNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CustomerStatus status = CustomerStatus.PROSPECT;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (isActive == null) isActive = true;
        if (status  == null) status   = CustomerStatus.PROSPECT;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
