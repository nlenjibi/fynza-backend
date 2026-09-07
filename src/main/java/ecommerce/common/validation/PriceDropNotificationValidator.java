package ecommerce.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PriceDropNotificationValidator implements ConstraintValidator<ValidPriceRange, PriceComparable> {

    @Override
    public boolean isValid(PriceComparable value, ConstraintValidatorContext context) {
        if (value == null || value.getOldPrice() == null || value.getNewPrice() == null) {
            return true;
        }
        boolean isValid = value.getNewPrice().compareTo(value.getOldPrice()) <= 0;
        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "New price (" + value.getNewPrice() + ") must be ≤ old price (" + value.getOldPrice() + ")")
                    .addConstraintViolation();
        }
        return isValid;
    }
}
