package ecommerce.common.validation;

import ecommerce.modules.wishlist.dto.PriceDropNotificationDto;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for PriceDropNotificationDto to ensure price drop is valid
 * (new price should be less than or equal to old price)
 */
public class PriceDropNotificationValidator implements ConstraintValidator<ValidPriceRange, PriceDropNotificationDto> {

    @Override
    public void initialize(ValidPriceRange annotation) {
        ConstraintValidator.super.initialize(annotation);
    }

    @Override
    public boolean isValid(PriceDropNotificationDto value, ConstraintValidatorContext context) {
        if (value == null || value.getOldPrice() == null || value.getNewPrice() == null) {
            return true; // Let @NotNull handle null validation
        }

        // New price should always be less than or equal to old price for a valid price drop
        boolean isValid = value.getNewPrice().compareTo(value.getOldPrice()) <= 0;

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "New price (" + value.getNewPrice() +
                            ") must be less than or equal to old price (" + value.getOldPrice() + ")")
                    .addConstraintViolation();
        }

        return isValid;
    }
}
