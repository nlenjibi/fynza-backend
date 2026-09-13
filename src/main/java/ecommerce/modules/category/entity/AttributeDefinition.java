package ecommerce.modules.category.entity;

import ecommerce.modules.category.enums.AttributeDataType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "attribute_definitions", indexes = {
        @Index(name = "idx_attr_def_category",   columnList = "category_id"),
        @Index(name = "idx_attr_def_code",       columnList = "code"),
        @Index(name = "idx_attr_def_filterable", columnList = "filterable")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttributeDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private AttributeDataType dataType;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "required", nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "filterable", nullable = false)
    @Builder.Default
    private Boolean filterable = false;

    @Column(name = "searchable", nullable = false)
    @Builder.Default
    private Boolean searchable = false;

    @Column(name = "variant_defining", nullable = false)
    @Builder.Default
    private Boolean variantDefining = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId  = UUID.randomUUID();
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
