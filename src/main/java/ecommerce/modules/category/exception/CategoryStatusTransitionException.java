package ecommerce.modules.category.exception;

import ecommerce.common.exception.BadRequestException;
import ecommerce.modules.category.enums.CategoryStatus;

public class CategoryStatusTransitionException extends BadRequestException {

    public CategoryStatusTransitionException(CategoryStatus from, CategoryStatus to) {
        super("Invalid category status transition: " + from + " → " + to);
    }
}
