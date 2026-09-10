package ecommerce.modules.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product_categories",
    uniqueConstraints = @UniqueConstraint(name = "uk_product_category", columnNames = {"product_id", "category_id"}),
    indexes = {
        @Index(name = "idx_product_categories_product",  columnList = "product_id"),
        @Index(name = "idx_product_categories_category", columnList = "category_id")
    })
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(name = "is_primary", nullable = false)
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (isPrimary == null) isPrimary = false;
    }
}
