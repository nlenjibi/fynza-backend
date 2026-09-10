package ecommerce.modules.category.entity;

import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Immutable
@Table(name = "v_category_summary")
@Getter
@NoArgsConstructor
public class CategorySummaryView {

    @Id
    private Long id;

    @Column(name = "public_id")
    private UUID publicId;

    @Column(name = "taxonomy_id")
    private Long taxonomyId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "name")
    private String name;

    @Column(name = "slug")
    private String slug;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private CategoryStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility")
    private CategoryVisibility visibility;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "media_id")
    private String mediaId;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "product_count")
    private Long productCount;

    @Column(name = "active_product_count")
    private Long activeProductCount;

    @Column(name = "child_category_count")
    private Long childCategoryCount;
}
