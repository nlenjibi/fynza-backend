package ecommerce.common.validation;

import ecommerce.modules.product.entity.Product;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;

public class PriceRangeValidator implements ConstraintValidator<ValidPriceRange, Product> {

    @Override
    public boolean isValid(Product value, ConstraintValidatorContext context) {
        if (value == null || value.getPrice() == null || value.getOriginalPrice() == null) {
            return true; // Let @NotNull handle null validation
        }

        // Current price should always be less than or equal to original price
        boolean isValid = value.getPrice().compareTo(value.getOriginalPrice()) <= 0;

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Current price (" + value.getPrice() + ") must be ≤ original price (" + value.getOriginalPrice() + ")")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
