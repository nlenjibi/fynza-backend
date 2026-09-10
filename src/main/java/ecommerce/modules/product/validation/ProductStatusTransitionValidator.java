package ecommerce.modules.product.validation;

import ecommerce.common.enums.ProductStatus;
import ecommerce.modules.product.exception.ProductStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Component
public class ProductStatusTransitionValidator {

    private static final Map<ProductStatus, Set<ProductStatus>> ALLOWED;

    static {
        ALLOWED = new EnumMap<>(ProductStatus.class);
        ALLOWED.put(ProductStatus.DRAFT,          EnumSet.of(ProductStatus.PENDING_REVIEW, ProductStatus.ARCHIVED, ProductStatus.DELETED));
        ALLOWED.put(ProductStatus.PENDING_REVIEW,  EnumSet.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE, ProductStatus.DRAFT, ProductStatus.SUSPENDED));
        ALLOWED.put(ProductStatus.ACTIVE,          EnumSet.of(ProductStatus.INACTIVE, ProductStatus.SUSPENDED, ProductStatus.ARCHIVED, ProductStatus.PENDING_REVIEW));
        ALLOWED.put(ProductStatus.INACTIVE,        EnumSet.of(ProductStatus.ACTIVE, ProductStatus.SUSPENDED, ProductStatus.ARCHIVED, ProductStatus.PENDING_REVIEW));
        ALLOWED.put(ProductStatus.SUSPENDED,       EnumSet.of(ProductStatus.ACTIVE, ProductStatus.INACTIVE, ProductStatus.ARCHIVED));
        ALLOWED.put(ProductStatus.ARCHIVED,        EnumSet.of(ProductStatus.DRAFT, ProductStatus.DELETED));
        ALLOWED.put(ProductStatus.DELETED,         EnumSet.noneOf(ProductStatus.class));
    }

    public void validate(ProductStatus from, ProductStatus to) {
        Set<ProductStatus> permitted = ALLOWED.getOrDefault(from, EnumSet.noneOf(ProductStatus.class));
        if (!permitted.contains(to)) {
            throw new ProductStatusTransitionException(from, to);
        }
    }

    public boolean isAllowed(ProductStatus from, ProductStatus to) {
        return ALLOWED.getOrDefault(from, EnumSet.noneOf(ProductStatus.class)).contains(to);
    }
}
