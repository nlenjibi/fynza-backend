package ecommerce.modules.store.spec;

import ecommerce.modules.store.dto.request.StoreSearchRequest;
import ecommerce.modules.store.entity.StoreSummaryView;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class StoreSummarySpec {

    private StoreSummarySpec() {}

    public static Specification<StoreSummaryView> fromRequest(StoreSearchRequest request) {
        return (root, query, cb) -> {
            if (request == null) return cb.conjunction();
            List<Predicate> predicates = new ArrayList<>();

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getStatus()));
            }
            if (request.getVisibility() != null) {
                predicates.add(cb.equal(root.get("visibility"), request.getVisibility()));
            }
            if (request.getIsActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), request.getIsActive()));
            }
            if (request.getQuery() != null && !request.getQuery().isBlank()) {
                String pattern = "%" + request.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("storeName")), pattern),
                        cb.like(cb.lower(root.get("slug")),      pattern)
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
