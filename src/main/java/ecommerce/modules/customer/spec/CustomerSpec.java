package ecommerce.modules.customer.spec;

import ecommerce.modules.customer.dto.request.CustomerSearchRequest;
import ecommerce.modules.customer.entity.Customer;
import ecommerce.modules.customer.enums.CustomerStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CustomerSpec {

    private CustomerSpec() {}

    public static Specification<Customer> fromRequest(CustomerSearchRequest request) {
        return (root, query, cb) -> {
            if (request == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getCustomerNumber() != null && !request.getCustomerNumber().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("customerNumber")),
                        "%" + request.getCustomerNumber().toLowerCase() + "%"));
            }
            predicates.add(cb.isTrue(root.get("isActive")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Customer> hasStatus(CustomerStatus status) {
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Customer> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }
}
