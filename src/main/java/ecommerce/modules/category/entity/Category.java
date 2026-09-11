package ecommerce.modules.category.entity;

import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "categories", indexes = {
        @Index(name = "idx_category_slug",        columnList = "slug"),
        @Index(name = "idx_category_parent_id",   columnList = "parent_category_id"),
        @Index(name = "idx_category_name",        columnList = "name"),
        @Index(name = "idx_category_is_active",   columnList = "is_active"),
        @Index(name = "idx_category_taxonomy_id", columnList = "taxonomy_id"),
        @Index(name = "idx_category_status",      columnList = "status"),
        @Index(name = "idx_category_visibility",  columnList = "visibility"),
        @Index(name = "idx_category_sort_order",  columnList = "sort_order")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId;

    @Column(name = "taxonomy_id")
    private Long taxonomyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "slug", nullable = false, unique = true, length = 150)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private CategoryStatus status = CategoryStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 30)
    @Builder.Default
    private CategoryVisibility visibility = CategoryVisibility.PUBLIC;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "media_id", length = 255)
    private String mediaId;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Category> subcategories = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        publicId   = UUID.randomUUID();
        createdAt  = Instant.now();
        updatedAt  = Instant.now();
        if (status     == null) status     = CategoryStatus.DRAFT;
        if (visibility == null) visibility = CategoryVisibility.PUBLIC;
        if (sortOrder  == null) sortOrder  = 0;
        if (isActive   == null) isActive   = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
