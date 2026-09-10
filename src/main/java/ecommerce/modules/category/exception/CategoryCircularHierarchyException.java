package ecommerce.modules.category.exception;

import ecommerce.common.exception.BadRequestException;

public class CategoryCircularHierarchyException extends BadRequestException {

    public CategoryCircularHierarchyException(String categoryName) {
        super("Moving '" + categoryName + "' would create a circular hierarchy");
    }
}
