package ecommerce.modules.product.spec;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.product.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ProductSpec {

    private ProductSpec() {}

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> nameOrDescriptionContains(String term) {
        return (root, query, cb) -> {
            if (term == null || term.isBlank()) return null;
            String like = "%" + term.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like)
            );
        };
    }

    public static Specification<Product> hasCategoryPublicId(UUID categoryPublicId) {
        return (root, query, cb) -> categoryPublicId == null ? null :
            cb.equal(root.get("category").get("publicId"), categoryPublicId);
    }

    public static Specification<Product> hasCategoryPublicIdIn(List<UUID> categoryPublicIds) {
        return (root, query, cb) -> (categoryPublicIds == null || categoryPublicIds.isEmpty()) ? null :
            root.get("category").get("publicId").in(categoryPublicIds);
    }

    public static Specification<Product> hasSellerPublicId(UUID sellerPublicId) {
        return (root, query, cb) -> sellerPublicId == null ? null :
            cb.equal(root.get("seller").get("publicId"), sellerPublicId);
    }

    public static Specification<Product> hasBrand(String brand) {
        return (root, query, cb) -> (brand == null || brand.isBlank()) ? null :
            cb.equal(cb.lower(root.get("brand")), brand.toLowerCase());
    }

    public static Specification<Product> hasBrandIn(List<String> brands) {
        return (root, query, cb) -> (brands == null || brands.isEmpty()) ? null :
            cb.lower(root.get("brand")).in(brands.stream().map(String::toLowerCase).toList());
    }

    public static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("price"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("price"), min);
            return cb.lessThanOrEqualTo(root.get("price"), max);
        };
    }

    public static Specification<Product> ratingBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("rating"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("rating"), min);
            return cb.lessThanOrEqualTo(root.get("rating"), max);
        };
    }

    public static Specification<Product> isInStock() {
        return (root, query, cb) -> cb.or(
            cb.greaterThan(root.get("stock"), 0),
            cb.greaterThan(root.get("availableQuantity"), 0)
        );
    }

    public static Specification<Product> isFeatured(Boolean featured) {
        return (root, query, cb) -> featured == null ? null : cb.equal(root.get("featured"), featured);
    }

    public static Specification<Product> isNew(Boolean isNew) {
        return (root, query, cb) -> isNew == null ? null : cb.equal(root.get("isNew"), isNew);
    }

    public static Specification<Product> isBestseller(Boolean bestseller) {
        return (root, query, cb) -> bestseller == null ? null : cb.equal(root.get("isBestseller"), bestseller);
    }

    public static Specification<Product> discountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("discount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("discount"), min);
            return cb.lessThanOrEqualTo(root.get("discount"), max);
        };
    }

    public static Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
