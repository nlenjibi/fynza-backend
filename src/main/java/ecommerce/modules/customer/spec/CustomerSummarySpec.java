package ecommerce.modules.customer.spec;

import ecommerce.modules.customer.dto.request.CustomerSearchRequest;
import ecommerce.modules.customer.entity.CustomerSummaryView;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class CustomerSummarySpec {

    private CustomerSummarySpec() {}

    public static Specification<CustomerSummaryView> fromRequest(CustomerSearchRequest request) {
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
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                String pattern = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")),  pattern),
                        cb.like(cb.lower(root.get("email")),     pattern)
                ));
            }
            predicates.add(cb.isTrue(root.get("isActive")));

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
