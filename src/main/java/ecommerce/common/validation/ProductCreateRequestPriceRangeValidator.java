package ecommerce.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

// Price fields were removed from CreateProductRequest; retained as no-op to satisfy compilation.
public class ProductCreateRequestPriceRangeValidator implements ConstraintValidator<ValidPriceRange, Object> {

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        return true;
    }
}
