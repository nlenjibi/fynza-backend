package ecommerce.common.validation;

import ecommerce.modules.product.dto.CreateProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductCreateRequestPriceRangeValidator implements ConstraintValidator<ValidPriceRange, CreateProductRequest> {

    @Override
    public boolean isValid(CreateProductRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getPrice() == null || value.getOriginalPrice() == null) {
            return true;
        }
        boolean isValid = value.getPrice().compareTo(value.getOriginalPrice()) <= 0;
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Current price (" + value.getPrice() +
                            ") must be ≤ original price (" + value.getOriginalPrice() + ")")
                    .addConstraintViolation();
        }
        return isValid;
    }
}
