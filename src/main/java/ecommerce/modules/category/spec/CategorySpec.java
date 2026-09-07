package ecommerce.modules.category.spec;

import ecommerce.modules.category.entity.Category;
import org.springframework.data.jpa.domain.Specification;

public final class CategorySpec {

    private CategorySpec() {}

    public static Specification<Category> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Category> isFeatured(Boolean featured) {
        return (root, query, cb) -> featured == null ? null : cb.equal(root.get("featured"), featured);
    }

    public static Specification<Category> nameContains(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) ? null :
            cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Category> hasParentPublicId(java.util.UUID parentPublicId) {
        return (root, query, cb) -> parentPublicId == null ? null :
            cb.equal(root.get("parentCategory").get("publicId"), parentPublicId);
    }

    public static Specification<Category> isTopLevel() {
        return (root, query, cb) -> cb.isNull(root.get("parentCategory"));
    }
}
