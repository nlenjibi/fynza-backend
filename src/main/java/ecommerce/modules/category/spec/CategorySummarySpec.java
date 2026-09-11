package ecommerce.modules.category.spec;

import ecommerce.modules.category.dto.request.CategorySearchRequest;
import ecommerce.modules.category.entity.CategorySummaryView;
import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.enums.CategoryVisibility;
import org.springframework.data.jpa.domain.Specification;

public final class CategorySummarySpec {

    private CategorySummarySpec() {}

    public static Specification<CategorySummaryView> from(CategorySearchRequest req) {
        return Specification
                .where(hasStatus(req.getStatus()))
                .and(hasVisibility(req.getVisibility()))
                .and(hasTaxonomy(req.getTaxonomyId()))
                .and(isActive(req.getIsActive()))
                .and(queryMatches(req.getQuery()));
    }

    private static Specification<CategorySummaryView> hasStatus(CategoryStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    private static Specification<CategorySummaryView> hasVisibility(CategoryVisibility visibility) {
        return (root, query, cb) -> visibility == null ? null : cb.equal(root.get("visibility"), visibility);
    }

    private static Specification<CategorySummaryView> hasTaxonomy(Long taxonomyId) {
        return (root, query, cb) -> taxonomyId == null ? null : cb.equal(root.get("taxonomyId"), taxonomyId);
    }

    private static Specification<CategorySummaryView> isActive(Boolean isActive) {
        return (root, query, cb) -> isActive == null ? null : cb.equal(root.get("isActive"), isActive);
    }

    private static Specification<CategorySummaryView> queryMatches(String q) {
        if (q == null || q.isBlank()) return null;
        String pattern = "%" + q.toLowerCase() + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("slug")), pattern)
        );
    }
}
