package ecommerce.modules.order.spec;

import ecommerce.common.enums.OrderStatus;
import ecommerce.modules.order.entity.Order;
import ecommerce.modules.order.entity.PaymentStatus;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;

public final class OrderSpec {

    private OrderSpec() {}

    public static Specification<Order> hasStatus(OrderStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Order> hasPaymentStatus(PaymentStatus paymentStatus) {
        return (root, query, cb) -> paymentStatus == null ? null : cb.equal(root.get("paymentStatus"), paymentStatus);
    }

    public static Specification<Order> hasCustomerPublicId(java.util.UUID customerPublicId) {
        return (root, query, cb) -> customerPublicId == null ? null :
            cb.equal(root.get("customer").get("publicId"), customerPublicId);
    }

    public static Specification<Order> orderNumberContains(String orderNumber) {
        return (root, query, cb) -> (orderNumber == null || orderNumber.isBlank()) ? null :
            cb.like(cb.lower(root.get("orderNumber")), "%" + orderNumber.toLowerCase() + "%");
    }

    public static Specification<Order> totalAmountBetween(BigDecimal min, BigDecimal max) {
        return (root, query, cb) -> {
            if (min == null && max == null) return null;
            if (min != null && max != null) return cb.between(root.get("totalAmount"), min, max);
            if (min != null) return cb.greaterThanOrEqualTo(root.get("totalAmount"), min);
            return cb.lessThanOrEqualTo(root.get("totalAmount"), max);
        };
    }

    public static Specification<Order> createdAfter(Instant date) {
        return (root, query, cb) -> date == null ? null : cb.greaterThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Order> createdBefore(Instant date) {
        return (root, query, cb) -> date == null ? null : cb.lessThanOrEqualTo(root.get("createdAt"), date);
    }

    public static Specification<Order> customerEmailContains(String email) {
        return (root, query, cb) -> (email == null || email.isBlank()) ? null :
            cb.like(cb.lower(root.get("customer").get("email")), "%" + email.toLowerCase() + "%");
    }

    public static Specification<Order> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Order> isHighValue(BigDecimal threshold) {
        BigDecimal limit = threshold != null ? threshold : new BigDecimal("500.00");
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("totalAmount"), limit);
    }

    public static Specification<Order> isOverdue() {
        Instant cutoff = Instant.now().minusSeconds(3 * 24 * 3600);
        return (root, query, cb) -> cb.and(
            cb.lessThanOrEqualTo(root.get("createdAt"), cutoff),
            root.get("status").in(OrderStatus.PENDING, OrderStatus.PROCESSING)
        );
    }
}
