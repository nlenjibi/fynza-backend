package ecommerce.modules.review.spec;

import ecommerce.modules.review.entity.Review;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class ReviewSpec {

    private ReviewSpec() {}

    public static Specification<Review> hasProductPublicId(UUID productPublicId) {
        return (root, query, cb) -> productPublicId == null ? null :
            cb.equal(root.get("product").get("publicId"), productPublicId);
    }

    public static Specification<Review> hasCustomerPublicId(UUID customerPublicId) {
        return (root, query, cb) -> customerPublicId == null ? null :
            cb.equal(root.get("customer").get("publicId"), customerPublicId);
    }

    public static Specification<Review> hasRating(Integer rating) {
        return (root, query, cb) -> rating == null ? null : cb.equal(root.get("rating"), rating);
    }

    public static Specification<Review> ratingBetween(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("rating"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("rating"), min);
            return cb.lessThanOrEqualTo(root.get("rating"), max);
        };
    }

    public static Specification<Review> isVerifiedPurchase(Boolean verified) {
        return (root, query, cb) -> verified == null ? null : cb.equal(root.get("verifiedPurchase"), verified);
    }

    public static Specification<Review> isApproved(Boolean approved) {
        return (root, query, cb) -> approved == null ? null : cb.equal(root.get("approved"), approved);
    }

    public static Specification<Review> textContains(String keyword) {
        return (root, query, cb) -> (keyword == null || keyword.isBlank()) ? null :
            cb.or(
                cb.like(cb.lower(root.get("title")), "%" + keyword.toLowerCase() + "%"),
                cb.like(cb.lower(root.get("comment")), "%" + keyword.toLowerCase() + "%")
            );
    }

    public static Specification<Review> createdAfter(Instant date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Review> createdBefore(Instant date) {
        return (root, query, cb) -> date == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Review> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
