package ecommerce.modules.user.spec;

import ecommerce.common.enums.Role;
import ecommerce.common.enums.UserStatus;
import ecommerce.modules.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpec {

    private UserSpec() {}

    public static Specification<User> hasRole(Role role) {
        return (root, query, cb) -> role == null ? null : cb.equal(root.get("role"), role);
    }

    public static Specification<User> hasStatus(UserStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<User> emailOrNameContains(String query) {
        return (root, qr, cb) -> {
            if (query == null || query.isBlank()) return null;
            String like = "%" + query.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("email")), like),
                cb.like(cb.lower(root.get("firstName")), like),
                cb.like(cb.lower(root.get("lastName")), like),
                cb.like(cb.lower(root.get("phone")), like)
            );
        };
    }

    public static Specification<User> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
