package ecommerce.modules.seller.spec;

import ecommerce.modules.seller.dto.request.SellerSearchRequest;
import ecommerce.modules.seller.entity.Seller;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class SellerSpec {

    private SellerSpec() {}

    public static Specification<Seller> fromRequest(SellerSearchRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.getQuery() != null && !req.getQuery().isBlank()) {
                String pattern = "%" + req.getQuery().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("displayName")), pattern),
                        cb.like(cb.lower(root.get("sellerNumber")), pattern)
                ));
            }
            if (req.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), req.getStatus()));
            }
            if (req.getSellerType() != null) {
                predicates.add(cb.equal(root.get("sellerType"), req.getSellerType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
