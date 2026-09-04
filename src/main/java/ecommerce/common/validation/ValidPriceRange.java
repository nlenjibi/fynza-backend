package ecommerce.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = {PriceRangeValidator.class, ProductCreateRequestPriceRangeValidator.class, PriceDropNotificationValidator.class})
@Documented
public @interface ValidPriceRange {
    String message() default "Discounted price must be less than original price";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
