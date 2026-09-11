package ecommerce.modules.category.validation;

import ecommerce.modules.category.enums.CategoryStatus;
import ecommerce.modules.category.exception.CategoryStatusTransitionException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Component
public class CategoryStatusTransitionValidator {

    private static final Map<CategoryStatus, Set<CategoryStatus>> ALLOWED = new EnumMap<>(CategoryStatus.class);

    static {
        ALLOWED.put(CategoryStatus.DRAFT,    Set.of(CategoryStatus.ACTIVE));
        ALLOWED.put(CategoryStatus.ACTIVE,   Set.of(CategoryStatus.INACTIVE, CategoryStatus.ARCHIVED));
        ALLOWED.put(CategoryStatus.INACTIVE, Set.of(CategoryStatus.ACTIVE,   CategoryStatus.ARCHIVED));
        ALLOWED.put(CategoryStatus.ARCHIVED, Set.of(CategoryStatus.DELETED));
        ALLOWED.put(CategoryStatus.DELETED,  Set.of());
    }

    public void validate(CategoryStatus from, CategoryStatus to) {
        if (!ALLOWED.getOrDefault(from, Set.of()).contains(to)) {
            throw new CategoryStatusTransitionException(from, to);
        }
    }
}
