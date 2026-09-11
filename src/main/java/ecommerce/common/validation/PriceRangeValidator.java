package ecommerce.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PriceRangeValidator implements ConstraintValidator<ValidPriceRange, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        // Price validation delegated to pricing module — no-op until wired
        return true;
    }
}
