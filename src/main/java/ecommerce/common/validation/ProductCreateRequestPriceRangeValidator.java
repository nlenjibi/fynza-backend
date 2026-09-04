package ecommerce.common.validation;

import ecommerce.modules.product.dto.CreateProductRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ProductCreateRequestPriceRangeValidator implements ConstraintValidator<ValidPriceRange, CreateProductRequest> {

    @Override
    public void initialize(ValidPriceRange annotation) {
        ConstraintValidator.super.initialize(annotation);
    }

    @Override
    public boolean isValid(CreateProductRequest value, ConstraintValidatorContext context) {
        if (value == null || value.getPrice() == null || value.getOriginalPrice() == null) {
            return true;
        }

        boolean isValid = value.getOriginalPrice().compareTo(value.getPrice()) >= 0;

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Original price (" + value.getOriginalPrice() +
                            ") must be greater than or equal to discounted price (" + value.getPrice() + ")")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
